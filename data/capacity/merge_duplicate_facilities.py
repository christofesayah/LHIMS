import argparse
import os
import re
from dataclasses import dataclass
from pathlib import Path

import psycopg


MERGE_GROUPS = [
    ["Lebanese Red Cross"],
    ["Hajj Medical Center", "Hajj Medical Center- Dr. Marcel Hajj M.D.", "Hajj"],
    ["Dr. Haissam Rafei's Eye Clinic", "عيادة د.هيثم الرافعي لطب وجراحة العين"],
    ["Doctors centre laboratory and radiology", "مركز الاطباء للأشعه"],
]


def parse_env_file(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    if not path.exists():
        return values
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip().strip('"').strip("'")
    return values


def db_settings(repo_root: Path) -> dict[str, str]:
    file_values = parse_env_file(repo_root / ".env")
    return {
        "host": os.getenv("DB_HOST", file_values.get("DB_HOST", "localhost")),
        "port": os.getenv("DB_PORT", file_values.get("DB_PORT", "5400")),
        "dbname": os.getenv("DB_NAME", file_values.get("DB_NAME", "lhims_db")),
        "user": os.getenv("DB_USER", file_values.get("DB_USER", "lhims_user")),
        "password": os.getenv("DB_PASS", file_values.get("DB_PASS", "lhims_pass")),
    }


def has_arabic(text: str) -> bool:
    return bool(re.search(r"[\u0600-\u06FF]", text))


def is_english_like(text: str) -> bool:
    return bool(re.search(r"[A-Za-z]", text)) and not has_arabic(text)


@dataclass
class FacilityRow:
    facility_id: int
    region_id: int
    name: str
    facility_type: str
    ownership_type: str
    latitude: float | None
    longitude: float | None
    has_geometry: bool
    capacity_count: int


def facility_score(row: FacilityRow) -> int:
    score = 0
    if is_english_like(row.name):
        score += 100
    if row.latitude is not None and row.longitude is not None:
        score += 30
    if row.has_geometry:
        score += 20
    score += min(row.capacity_count, 5) * 10
    score += min(len(row.name), 80)
    return score


def choose_keeper(rows: list[FacilityRow]) -> FacilityRow:
    return sorted(rows, key=lambda row: (facility_score(row), -row.facility_id), reverse=True)[0]


def canonical_english_name(rows: list[FacilityRow], fallback: str) -> str:
    english_candidates = [row.name for row in rows if is_english_like(row.name)]
    if not english_candidates:
        return fallback
    return sorted(english_candidates, key=lambda n: (-len(n), n.lower()))[0]


def merge_capacity_rows(cur: psycopg.Cursor, donor_id: int, keeper_id: int) -> tuple[int, int]:
    moved = 0
    merged = 0
    cur.execute(
        """
        SELECT capacity_id, reporting_date, total_beds, icu_beds, doctors_count, nurses_count, operational_status
        FROM facility_capacity
        WHERE facility_id = %s
        ORDER BY reporting_date, capacity_id
        """,
        (donor_id,),
    )
    donor_rows = cur.fetchall()
    for capacity_id, reporting_date, total_beds, icu_beds, doctors_count, nurses_count, operational_status in donor_rows:
        cur.execute(
            """
            SELECT capacity_id, total_beds, icu_beds, doctors_count, nurses_count, operational_status
            FROM facility_capacity
            WHERE facility_id = %s AND reporting_date = %s
            LIMIT 1
            """,
            (keeper_id, reporting_date),
        )
        keeper_existing = cur.fetchone()
        if not keeper_existing:
            cur.execute("UPDATE facility_capacity SET facility_id = %s WHERE capacity_id = %s", (keeper_id, capacity_id))
            moved += 1
            continue

        keeper_capacity_id, k_total_beds, k_icu_beds, k_doctors_count, k_nurses_count, k_operational_status = keeper_existing
        cur.execute(
            """
            UPDATE facility_capacity
            SET total_beds = COALESCE(total_beds, %s),
                icu_beds = COALESCE(icu_beds, %s),
                doctors_count = COALESCE(doctors_count, %s),
                nurses_count = COALESCE(nurses_count, %s),
                operational_status = COALESCE(operational_status, %s)
            WHERE capacity_id = %s
            """,
            (total_beds, icu_beds, doctors_count, nurses_count, operational_status, keeper_capacity_id),
        )
        cur.execute("DELETE FROM facility_capacity WHERE capacity_id = %s", (capacity_id,))
        merged += 1

    return moved, merged


def load_rows_for_names(cur: psycopg.Cursor, names: list[str]) -> list[FacilityRow]:
    cur.execute(
        """
        SELECT hf.facility_id, hf.region_id, hf.name, hf.facility_type::text, hf.ownership_type::text,
               hf.latitude, hf.longitude, (hf.geometry IS NOT NULL) AS has_geometry,
               (SELECT COUNT(*) FROM facility_capacity fc WHERE fc.facility_id = hf.facility_id) AS capacity_count
        FROM health_facilities hf
        WHERE hf.name = ANY(%s)
        ORDER BY hf.facility_id
        """,
        (names,),
    )
    rows = []
    for row in cur.fetchall():
        rows.append(
            FacilityRow(
                facility_id=int(row[0]),
                region_id=int(row[1]),
                name=row[2],
                facility_type=row[3],
                ownership_type=row[4],
                latitude=row[5],
                longitude=row[6],
                has_geometry=bool(row[7]),
                capacity_count=int(row[8]),
            )
        )
    return rows


def main() -> None:
    parser = argparse.ArgumentParser(description="Merge known duplicate facilities while preserving location/capacity.")
    parser.add_argument("--repo-root", default=str(Path(__file__).resolve().parents[2]))
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve()
    db = db_settings(repo_root)

    with psycopg.connect(**db) as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT COUNT(*) FROM health_facilities")
            before_facilities = int(cur.fetchone()[0])
            cur.execute("SELECT COUNT(*) FROM facility_capacity")
            before_capacity = int(cur.fetchone()[0])

            summary_lines: list[str] = []
            merged_groups = 0
            deleted_rows = 0
            moved_capacity_total = 0
            merged_capacity_total = 0

            for group in MERGE_GROUPS:
                rows = load_rows_for_names(cur, group)
                if len(rows) < 2:
                    continue

                keeper = choose_keeper(rows)
                donors = [row for row in rows if row.facility_id != keeper.facility_id]
                canonical_name = canonical_english_name(rows, keeper.name)
                merged_groups += 1

                best_location = next(
                    (row for row in rows if row.latitude is not None and row.longitude is not None and row.has_geometry),
                    next((row for row in rows if row.latitude is not None and row.longitude is not None), None),
                )
                target_region_id = best_location.region_id if best_location else keeper.region_id
                target_latitude = best_location.latitude if best_location else keeper.latitude
                target_longitude = best_location.longitude if best_location else keeper.longitude

                target_type = "HOSPITAL" if any(row.facility_type == "HOSPITAL" for row in rows) else keeper.facility_type
                target_ownership = "PUBLIC" if any(row.ownership_type == "PUBLIC" for row in rows) else keeper.ownership_type

                cur.execute(
                    """
                    UPDATE health_facilities
                    SET name = %s,
                        facility_type = %s,
                        ownership_type = %s,
                        region_id = %s,
                        latitude = %s,
                        longitude = %s,
                        geometry = CASE
                                     WHEN %s IS NULL OR %s IS NULL THEN geometry
                                     ELSE ST_SetSRID(ST_Point(%s, %s), 4326)
                                   END
                    WHERE facility_id = %s
                    """,
                    (
                        canonical_name,
                        target_type,
                        target_ownership,
                        target_region_id,
                        target_latitude,
                        target_longitude,
                        target_longitude,
                        target_latitude,
                        target_longitude,
                        target_latitude,
                        keeper.facility_id,
                    ),
                )

                donor_ids = [row.facility_id for row in donors]
                if donor_ids:
                    cur.execute("UPDATE scenario_actions SET facility_id = %s WHERE facility_id = ANY(%s)", (keeper.facility_id, donor_ids))

                moved_capacity_group = 0
                merged_capacity_group = 0
                for donor in donors:
                    moved, merged = merge_capacity_rows(cur, donor.facility_id, keeper.facility_id)
                    moved_capacity_group += moved
                    merged_capacity_group += merged
                    cur.execute("DELETE FROM health_facilities WHERE facility_id = %s", (donor.facility_id,))
                    deleted_rows += 1

                moved_capacity_total += moved_capacity_group
                merged_capacity_total += merged_capacity_group
                summary_lines.append(
                    f"keeper={keeper.facility_id}:{canonical_name} donors={donor_ids} moved_capacity={moved_capacity_group} merged_capacity={merged_capacity_group}"
                )

            conn.commit()

            cur.execute("SELECT COUNT(*) FROM health_facilities")
            after_facilities = int(cur.fetchone()[0])
            cur.execute("SELECT COUNT(*) FROM facility_capacity")
            after_capacity = int(cur.fetchone()[0])

    print("=== MERGE SUMMARY ===")
    print(f"groups_merged={merged_groups}")
    print(f"rows_deleted={deleted_rows}")
    print(f"moved_capacity_rows={moved_capacity_total}")
    print(f"merged_capacity_rows={merged_capacity_total}")
    print(f"facilities_before={before_facilities}")
    print(f"facilities_after={after_facilities}")
    print(f"capacity_before={before_capacity}")
    print(f"capacity_after={after_capacity}")
    print("details:")
    for line in summary_lines:
        print(line)


if __name__ == "__main__":
    main()
