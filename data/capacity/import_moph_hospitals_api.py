import argparse
import json
import os
import re
import unicodedata
import urllib.parse
import urllib.request
from dataclasses import dataclass
from datetime import date, datetime
from pathlib import Path
from typing import Any

import psycopg


API_URL = "https://maps.moph.gov.lb/server/rest/services/allservices/FeatureServer/10/query"
API_OUT_FIELDS = (
    "OBJECTID,Facility_En,Facility_Ar,Facility_Type,ownership,Caza,Mohafaza,"
    "Medicine_bed,Surgery_bed,BGYN_bed,Pediatrics_bed,ICU_CCU_bed,Nb_Doctors,Nb_Nurses,X,Y"
)


CAZA_ALIASES = {
    "batroun": "El Batroun",
    "bint jbeil": "Bent Jbeil",
    "bent jbail": "Bent Jbeil",
    "bent jbeil": "Bent Jbeil",
    "hermel": "El Hermel",
    "koura": "El Koura",
    "metn": "El Meten",
    "el metn": "El Meten",
    "jbail": "Jbeil",
    "jbayl": "Jbeil",
    "kesrouan": "Kesrwane",
    "keserwan": "Kesrwane",
    "kesrwan": "Kesrwane",
    "kesseroune": "Kesrwane",
    "ech chouf": "Chouf",
    "nabatieh": "El Nabatieh",
    "nabatiyeh": "El Nabatieh",
    "nabatyeh": "El Nabatieh",
    "minieh dinnieh": "El Minieh-Dennie",
    "minieh denniyeh": "El Minieh-Dennie",
    "minieh dennieh": "El Minieh-Dennie",
}


PUBLIC_OWNERSHIP_TOKENS = {"public", "government", "gov", "military", "ministry", "state"}


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


def normalize_text(value: str) -> str:
    text = unicodedata.normalize("NFKD", value).encode("ascii", "ignore").decode("ascii")
    text = text.lower()
    text = re.sub(r"[^a-z0-9]+", " ", text)
    return " ".join(text.split())


def normalize_name(value: str) -> str:
    text = normalize_text(value)
    replacements = {
        "hospital": "",
        "hopital": "",
        "medical center": "med center",
        "centre": "center",
        "clinic": "",
        "clinique": "",
        "university": "univ",
        "dr ": "doctor ",
    }
    for old, new in replacements.items():
        text = text.replace(old, new)
    return " ".join(text.split())


def parse_optional_int(value: Any) -> int | None:
    if value is None:
        return None
    if isinstance(value, int):
        return value
    if isinstance(value, float):
        return int(value)
    text = str(value).strip()
    if not text:
        return None
    try:
        return int(float(text))
    except ValueError:
        return None


def compute_total_beds(attrs: dict[str, Any]) -> int | None:
    parts = [
        parse_optional_int(attrs.get("Medicine_bed")),
        parse_optional_int(attrs.get("Surgery_bed")),
        parse_optional_int(attrs.get("BGYN_bed")),
        parse_optional_int(attrs.get("Pediatrics_bed")),
    ]
    present = [part for part in parts if part is not None]
    return sum(present) if present else None


def ownership_to_enum(value: str | None) -> str:
    normalized = normalize_text(value or "")
    if any(token in normalized for token in PUBLIC_OWNERSHIP_TOKENS):
        return "PUBLIC"
    return "PRIVATE"


def facility_type_to_enum(value: str | None) -> str:
    normalized = normalize_text(value or "")
    if "hospital" in normalized or "mustashfa" in normalized:
        return "HOSPITAL"
    return "PHC"


def resolve_caza_name(raw_caza: str | None, normalized_to_caza: dict[str, str]) -> str | None:
    if not raw_caza:
        return None
    normalized = normalize_text(raw_caza)
    if normalized in normalized_to_caza:
        return normalized_to_caza[normalized]
    alias = CAZA_ALIASES.get(normalized)
    if alias:
        return alias
    return None


def fetch_all_features() -> list[dict[str, Any]]:
    all_features: list[dict[str, Any]] = []
    offset = 0
    page_size = 1000

    while True:
        params = {
            "where": "1=1",
            "outFields": API_OUT_FIELDS,
            "f": "json",
            "resultRecordCount": str(page_size),
            "resultOffset": str(offset),
            "returnGeometry": "false",
        }
        url = f"{API_URL}?{urllib.parse.urlencode(params)}"
        with urllib.request.urlopen(url, timeout=60) as response:
            payload = json.loads(response.read().decode("utf-8"))

        features = payload.get("features", [])
        if not features:
            break
        all_features.extend(features)
        if not payload.get("exceededTransferLimit"):
            break
        offset += len(features)

    return all_features


@dataclass
class ExistingFacility:
    facility_id: int
    region_id: int
    name: str
    facility_type: str
    latitude: float | None
    longitude: float | None
    capacity_rows: int


def load_existing_facilities(cur: psycopg.Cursor) -> dict[str, list[ExistingFacility]]:
    cur.execute(
        """
        SELECT hf.facility_id, hf.region_id, hf.name, hf.facility_type::text, hf.latitude, hf.longitude,
               (SELECT COUNT(*) FROM facility_capacity fc WHERE fc.facility_id = hf.facility_id) AS cap_rows
        FROM health_facilities hf
        """
    )
    by_name: dict[str, list[ExistingFacility]] = {}
    for row in cur.fetchall():
        facility = ExistingFacility(
            facility_id=int(row[0]),
            region_id=int(row[1]),
            name=row[2],
            facility_type=row[3],
            latitude=row[4],
            longitude=row[5],
            capacity_rows=int(row[6]),
        )
        key = normalize_name(facility.name)
        by_name.setdefault(key, []).append(facility)
    return by_name


def choose_existing_facility(
    candidates: list[ExistingFacility],
    target_region_id: int | None,
    target_type: str,
) -> ExistingFacility:
    scored = sorted(
        candidates,
        key=lambda c: (
            1 if target_region_id is not None and c.region_id == target_region_id else 0,
            1 if c.facility_type == target_type else 0,
            1 if c.latitude is not None and c.longitude is not None else 0,
            c.capacity_rows,
            -c.facility_id,
        ),
        reverse=True,
    )
    return scored[0]


def upsert_capacity(
    cur: psycopg.Cursor,
    facility_id: int,
    reporting_date: date,
    total_beds: int | None,
    icu_beds: int | None,
    doctors_count: int | None,
    nurses_count: int | None,
) -> tuple[int, int]:
    cur.execute(
        """
        SELECT capacity_id
        FROM facility_capacity
        WHERE facility_id = %s AND reporting_date = %s
        LIMIT 1
        """,
        (facility_id, reporting_date),
    )
    row = cur.fetchone()
    if row:
        cur.execute(
            """
            UPDATE facility_capacity
            SET total_beds = COALESCE(%s, total_beds),
                icu_beds = COALESCE(%s, icu_beds),
                doctors_count = COALESCE(%s, doctors_count),
                nurses_count = COALESCE(%s, nurses_count),
                operational_status = 'OPERATIONAL'
            WHERE capacity_id = %s
            """,
            (total_beds, icu_beds, doctors_count, nurses_count, int(row[0])),
        )
        return 0, 1

    cur.execute(
        """
        INSERT INTO facility_capacity (
            facility_id, reporting_date, total_beds, icu_beds, doctors_count, nurses_count, operational_status
        )
        VALUES (%s, %s, %s, %s, %s, %s, 'OPERATIONAL')
        """,
        (facility_id, reporting_date, total_beds, icu_beds, doctors_count, nurses_count),
    )
    return 1, 0


def main() -> None:
    parser = argparse.ArgumentParser(description="Import MoPH hospitals API into health_facilities and facility_capacity.")
    parser.add_argument("--repo-root", default=str(Path(__file__).resolve().parents[2]))
    parser.add_argument(
        "--reporting-date",
        default=date.today().isoformat(),
        help="Reporting date for inserted/updated facility_capacity rows (YYYY-MM-DD).",
    )
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve()
    reporting_date = datetime.strptime(args.reporting_date, "%Y-%m-%d").date()

    features = fetch_all_features()

    db = db_settings(repo_root)
    with psycopg.connect(**db) as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT COUNT(*) FROM health_facilities")
            before_facilities = int(cur.fetchone()[0])
            cur.execute("SELECT COUNT(*) FROM facility_capacity")
            before_capacity = int(cur.fetchone()[0])

            cur.execute("SELECT region_id, name FROM regions WHERE type = 'CAZA'")
            caza_by_name = {name: int(region_id) for region_id, name in cur.fetchall()}
            normalized_to_caza = {normalize_text(name): name for name in caza_by_name}

            existing_by_name = load_existing_facilities(cur)

            inserted_facilities = 0
            updated_facilities = 0
            inserted_capacity = 0
            updated_capacity = 0
            skipped_no_name = 0
            skipped_no_region = 0
            skipped_non_hospital = 0

            for feature in features:
                attrs = feature.get("attributes", {})
                name_en = (attrs.get("Facility_En") or "").strip()
                if not name_en:
                    skipped_no_name += 1
                    continue

                facility_type = facility_type_to_enum(attrs.get("Facility_Type"))
                if facility_type != "HOSPITAL":
                    skipped_non_hospital += 1
                    continue

                caza_name = resolve_caza_name(attrs.get("Caza"), normalized_to_caza)
                if not caza_name:
                    skipped_no_region += 1
                    continue
                region_id = caza_by_name[caza_name]

                ownership_type = ownership_to_enum(attrs.get("ownership"))
                latitude = attrs.get("Y")
                longitude = attrs.get("X")
                if latitude is not None:
                    latitude = float(latitude)
                if longitude is not None:
                    longitude = float(longitude)

                name_key = normalize_name(name_en)
                candidates = existing_by_name.get(name_key, [])

                if candidates:
                    existing = choose_existing_facility(candidates, region_id, facility_type)
                    cur.execute(
                        """
                        UPDATE health_facilities
                        SET name = %s,
                            facility_type = %s,
                            ownership_type = %s,
                            region_id = %s,
                            latitude = CASE WHEN %s IS NULL THEN latitude ELSE %s END,
                            longitude = CASE WHEN %s IS NULL THEN longitude ELSE %s END,
                            geometry = CASE
                                        WHEN %s IS NULL OR %s IS NULL THEN geometry
                                        ELSE ST_SetSRID(ST_Point(%s, %s), 4326)
                                      END
                        WHERE facility_id = %s
                        """,
                        (
                            name_en,
                            facility_type,
                            ownership_type,
                            region_id,
                            latitude,
                            latitude,
                            longitude,
                            longitude,
                            longitude,
                            latitude,
                            longitude,
                            latitude,
                            existing.facility_id,
                        ),
                    )
                    facility_id = existing.facility_id
                    updated_facilities += 1
                else:
                    cur.execute(
                        """
                        INSERT INTO health_facilities (
                            region_id, name, facility_type, ownership_type, latitude, longitude, geometry
                        )
                        VALUES (
                            %s, %s, %s, %s, %s, %s,
                            CASE
                                WHEN %s IS NULL OR %s IS NULL THEN NULL
                                ELSE ST_SetSRID(ST_Point(%s, %s), 4326)
                            END
                        )
                        RETURNING facility_id
                        """,
                        (
                            region_id,
                            name_en,
                            facility_type,
                            ownership_type,
                            latitude,
                            longitude,
                            longitude,
                            latitude,
                            longitude,
                            latitude,
                        ),
                    )
                    facility_id = int(cur.fetchone()[0])
                    inserted_facilities += 1
                    existing_by_name.setdefault(name_key, []).append(
                        ExistingFacility(
                            facility_id=facility_id,
                            region_id=region_id,
                            name=name_en,
                            facility_type=facility_type,
                            latitude=latitude,
                            longitude=longitude,
                            capacity_rows=0,
                        )
                    )

                total_beds = compute_total_beds(attrs)
                icu_beds = parse_optional_int(attrs.get("ICU_CCU_bed"))
                doctors_count = parse_optional_int(attrs.get("Nb_Doctors"))
                nurses_count = parse_optional_int(attrs.get("Nb_Nurses"))
                ins, upd = upsert_capacity(
                    cur=cur,
                    facility_id=facility_id,
                    reporting_date=reporting_date,
                    total_beds=total_beds,
                    icu_beds=icu_beds,
                    doctors_count=doctors_count,
                    nurses_count=nurses_count,
                )
                inserted_capacity += ins
                updated_capacity += upd

            conn.commit()

            cur.execute("SELECT COUNT(*) FROM health_facilities")
            after_facilities = int(cur.fetchone()[0])
            cur.execute("SELECT COUNT(*) FROM facility_capacity")
            after_capacity = int(cur.fetchone()[0])
            cur.execute(
                """
                SELECT COUNT(*)
                FROM health_facilities
                WHERE facility_type = 'HOSPITAL'
                """
            )
            total_hospitals = int(cur.fetchone()[0])
            cur.execute(
                """
                SELECT COUNT(*)
                FROM facility_capacity
                WHERE reporting_date = %s
                """,
                (reporting_date,),
            )
            snapshot_rows = int(cur.fetchone()[0])

    print("=== MOPH API IMPORT SUMMARY ===")
    print(f"features_fetched={len(features)}")
    print(f"reporting_date={reporting_date.isoformat()}")
    print(f"inserted_facilities={inserted_facilities}")
    print(f"updated_facilities={updated_facilities}")
    print(f"inserted_capacity_rows={inserted_capacity}")
    print(f"updated_capacity_rows={updated_capacity}")
    print(f"skipped_no_name={skipped_no_name}")
    print(f"skipped_no_region={skipped_no_region}")
    print(f"skipped_non_hospital={skipped_non_hospital}")
    print(f"facilities_before={before_facilities}")
    print(f"facilities_after={after_facilities}")
    print(f"capacity_before={before_capacity}")
    print(f"capacity_after={after_capacity}")
    print(f"total_hospitals_after={total_hospitals}")
    print(f"capacity_rows_for_reporting_date={snapshot_rows}")


if __name__ == "__main__":
    main()
