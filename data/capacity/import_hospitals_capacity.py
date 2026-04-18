import argparse
import csv
import os
import re
import unicodedata
from dataclasses import dataclass
from datetime import date
from pathlib import Path

import psycopg


REPORTING_DATE = date(2021, 4, 18)


DISTRICT_ALIASES = {
    "beirut": "Beirut",
    "tripoli": "Tripoli",
    "trablos": "Tripoli",
    "akkar": "Akkar",
    "aley": "Aley",
    "baabda": "Baabda",
    "baalbek": "Baalbek",
    "baalbeck": "Baalbek",
    "bcharre": "Bcharre",
    "bsharre": "Bcharre",
    "bcharr": "Bcharre",
    "bent jbail": "Bent Jbeil",
    "bent jbeil": "Bent Jbeil",
    "bint jbeil": "Bent Jbeil",
    "chouf": "Chouf",
    "el chouf": "Chouf",
    "shouf": "Chouf",
    "batroun": "El Batroun",
    "el batroun": "El Batroun",
    "hermel": "El Hermel",
    "el hermel": "El Hermel",
    "koura": "El Koura",
    "el koura": "El Koura",
    "metn": "El Meten",
    "el metn": "El Meten",
    "el meten": "El Meten",
    "meten": "El Meten",
    "dennieh": "El Minieh-Dennie",
    "dinnieh": "El Minieh-Dennie",
    "minieh": "El Minieh-Dennie",
    "minieh dennieh": "El Minieh-Dennie",
    "nabatieh": "El Nabatieh",
    "nabatiyeh": "El Nabatieh",
    "nabatyeh": "El Nabatieh",
    "hasbaya": "Hasbaya",
    "jbeil": "Jbeil",
    "byblos": "Jbeil",
    "jezzine": "Jezzine",
    "kesrouan": "Kesrwane",
    "keserwan": "Kesrwane",
    "kesrwane": "Kesrwane",
    "marjaayoun": "Marjaayoun",
    "marjeyoun": "Marjaayoun",
    "rashaya": "Rachaya",
    "rachaya": "Rachaya",
    "saida": "Saida",
    "sidon": "Saida",
    "sour": "Sour",
    "tyr": "Sour",
    "tyre": "Sour",
    "west bekaa": "West Bekaa",
    "west bekka": "West Bekaa",
    "zahle": "Zahle",
    "zgharta": "Zgharta",
}


LOCATION_DEFAULTS = {
    "beirut": "Beirut",
    "metn/baabda": "Baabda",
    "north lebanon": "Tripoli",
    "south lebanon": "Saida",
    "bekaa": "Zahle",
    "chouf/aley": "Aley",
    "kesrouan/jbeil": "Kesrwane",
}


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
    ascii_text = unicodedata.normalize("NFKD", value).encode("ascii", "ignore").decode("ascii")
    lowered = ascii_text.lower()
    stripped = re.sub(r"[^a-z0-9]+", " ", lowered)
    return " ".join(stripped.split())


def normalize_hospital_name(value: str) -> str:
    text = normalize_text(value)
    replacements = {
        "hospital": "",
        "hopital": "",
        "centre": "center",
        "medical center": "med center",
        "dr ": "doctor ",
        " saint ": " st ",
    }
    for old, new in replacements.items():
        text = text.replace(old, new)
    return " ".join(text.split())


def is_public_hospital(name: str) -> bool:
    lowered = normalize_text(name)
    return "government" in lowered or "gov" in lowered or "public" in lowered


@dataclass
class HospitalRecord:
    source_id: str
    name: str
    location: str
    address: str
    beds: int
    normalized_name: str


def read_hospital_rows(path: Path) -> list[HospitalRecord]:
    rows = csv.DictReader(path.read_text(encoding="utf-8-sig").splitlines())
    hospitals: list[HospitalRecord] = []
    seen_norm_names: set[str] = set()
    for row in rows:
        name = (row.get("Hospital") or "").strip()
        if not name:
            continue
        beds = int((row.get("Number of Beds") or "0").strip() or "0")
        normalized = normalize_hospital_name(name)
        if normalized in seen_norm_names:
            continue
        seen_norm_names.add(normalized)
        hospitals.append(
            HospitalRecord(
                source_id=(row.get("Id") or "").strip(),
                name=name,
                location=(row.get("Location") or "").strip(),
                address=(row.get("Address") or "").strip(),
                beds=beds,
                normalized_name=normalized,
            )
        )
    return hospitals


def load_regions(cur: psycopg.Cursor) -> tuple[dict[str, int], dict[str, str]]:
    cur.execute("SELECT region_id, name FROM regions WHERE type = 'CAZA'")
    region_by_name: dict[str, int] = {}
    normalized_name_map: dict[str, str] = {}
    for region_id, name in cur.fetchall():
        region_by_name[name] = int(region_id)
        normalized_name_map[normalize_text(name)] = name
    return region_by_name, normalized_name_map


def detect_district(record: HospitalRecord, normalized_regions: dict[str, str]) -> str | None:
    haystack = normalize_text(f"{record.location} {record.address}")

    for key, district in DISTRICT_ALIASES.items():
        if key in haystack:
            return district

    for normalized_region_name, canonical_name in normalized_regions.items():
        if normalized_region_name in haystack:
            return canonical_name

    fallback = LOCATION_DEFAULTS.get(normalize_text(record.location))
    return fallback


def load_existing_hospitals(cur: psycopg.Cursor) -> tuple[dict[str, int], int]:
    cur.execute(
        """
        SELECT facility_id, name
        FROM health_facilities
        WHERE facility_type = 'HOSPITAL'
        """
    )
    by_normalized_name: dict[str, int] = {}
    for facility_id, name in cur.fetchall():
        by_normalized_name[normalize_hospital_name(name)] = int(facility_id)
    return by_normalized_name, len(by_normalized_name)


def ensure_capacity_row(cur: psycopg.Cursor, facility_id: int, beds: int) -> bool:
    cur.execute(
        """
        SELECT 1
        FROM facility_capacity
        WHERE facility_id = %s AND reporting_date = %s
        LIMIT 1
        """,
        (facility_id, REPORTING_DATE),
    )
    if cur.fetchone():
        return False
    cur.execute(
        """
        INSERT INTO facility_capacity (
            facility_id, reporting_date, total_beds, icu_beds, doctors_count, nurses_count, operational_status
        )
        VALUES (%s, %s, %s, NULL, NULL, NULL, 'OPERATIONAL')
        """,
        (facility_id, REPORTING_DATE, beds),
    )
    return True


def main() -> None:
    parser = argparse.ArgumentParser(description="Import Lebanon hospital capacity rows into health_facilities/facility_capacity.")
    parser.add_argument("--repo-root", default=str(Path(__file__).resolve().parents[2]))
    parser.add_argument("--csv-path", default="data/capacity/hospitals_beds_2021.csv")
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve()
    csv_path = (repo_root / args.csv_path).resolve() if not Path(args.csv_path).is_absolute() else Path(args.csv_path)

    hospital_rows = read_hospital_rows(csv_path)
    source_hospital_count = len(hospital_rows)
    source_normalized_names = {row.normalized_name for row in hospital_rows}

    db = db_settings(repo_root)
    with psycopg.connect(**db) as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT COUNT(*) FROM health_facilities WHERE facility_type = 'HOSPITAL'")
            before_hospital_count = int(cur.fetchone()[0])
            cur.execute("SELECT COUNT(*) FROM facility_capacity")
            before_capacity_count = int(cur.fetchone()[0])

            region_by_name, normalized_regions = load_regions(cur)
            existing_by_name, _ = load_existing_hospitals(cur)

            matched_existing = 0
            inserted_hospitals = 0
            inserted_capacities = 0
            skipped_no_district = 0
            removed_non_source_hospitals = 0

            for record in hospital_rows:
                facility_id = existing_by_name.get(record.normalized_name)
                if facility_id is not None:
                    matched_existing += 1
                else:
                    district_name = detect_district(record, normalized_regions)
                    if not district_name:
                        skipped_no_district += 1
                        continue
                    region_id = region_by_name.get(district_name)
                    if region_id is None:
                        skipped_no_district += 1
                        continue

                    ownership_type = "PUBLIC" if is_public_hospital(record.name) else "PRIVATE"
                    cur.execute(
                        """
                        INSERT INTO health_facilities (
                            region_id, name, facility_type, ownership_type, latitude, longitude, geometry
                        )
                        VALUES (%s, %s, 'HOSPITAL', %s, NULL, NULL, NULL)
                        RETURNING facility_id
                        """,
                        (region_id, record.name, ownership_type),
                    )
                    facility_id = int(cur.fetchone()[0])
                    existing_by_name[record.normalized_name] = facility_id
                    inserted_hospitals += 1

                if ensure_capacity_row(cur, facility_id, record.beds):
                    inserted_capacities += 1

            cur.execute(
                """
                SELECT facility_id, name
                FROM health_facilities
                WHERE facility_type = 'HOSPITAL'
                """
            )
            removable_ids: list[int] = []
            for facility_id, name in cur.fetchall():
                if normalize_hospital_name(name) not in source_normalized_names:
                    removable_ids.append(int(facility_id))

            if removable_ids:
                cur.execute("DELETE FROM facility_capacity WHERE facility_id = ANY(%s)", (removable_ids,))
                cur.execute("DELETE FROM health_facilities WHERE facility_id = ANY(%s)", (removable_ids,))
                removed_non_source_hospitals = len(removable_ids)

            conn.commit()

            cur.execute("SELECT COUNT(*) FROM health_facilities WHERE facility_type = 'HOSPITAL'")
            after_hospital_count = int(cur.fetchone()[0])
            cur.execute("SELECT COUNT(*) FROM facility_capacity")
            after_capacity_count = int(cur.fetchone()[0])
            cur.execute(
                """
                SELECT COUNT(*)
                FROM health_facilities
                WHERE facility_type = 'HOSPITAL'
                  AND region_id IN (SELECT region_id FROM regions WHERE type = 'CAZA')
                """
            )
            hospitals_in_caza = int(cur.fetchone()[0])

    print("=== IMPORT SUMMARY ===")
    print(f"source_unique_hospitals={source_hospital_count}")
    print(f"matched_existing={matched_existing}")
    print(f"inserted_hospitals={inserted_hospitals}")
    print(f"skipped_no_district={skipped_no_district}")
    print(f"inserted_capacity_rows={inserted_capacities}")
    print(f"removed_non_source_hospitals={removed_non_source_hospitals}")
    print(f"hospitals_before={before_hospital_count}")
    print(f"hospitals_after={after_hospital_count}")
    print(f"hospitals_in_caza_after={hospitals_in_caza}")
    print(f"capacity_before={before_capacity_count}")
    print(f"capacity_after={after_capacity_count}")


if __name__ == "__main__":
    main()
