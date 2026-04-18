import argparse
import csv
import json
import os
from pathlib import Path
from typing import Any

import psycopg


PHC_TAGS = {
    "clinic",
    "doctor",
    "doctors",
    "centre",
    "center",
    "laboratory",
    "lab",
    "yes",
}


PUBLIC_OPERATOR_TOKENS = {
    "public",
    "government",
    "gov",
    "ministry",
    "municipal",
    "military",
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


def normalize_name(value: str | None) -> str:
    if value is None:
        return ""
    return " ".join(value.strip().split()).lower()


def classify_facility_type(amenity: str | None, healthcare: str | None) -> str | None:
    a = (amenity or "").strip().lower()
    h = (healthcare or "").strip().lower()
    if a == "hospital" or h == "hospital":
        return "HOSPITAL"
    if a in PHC_TAGS or h in PHC_TAGS:
        return "PHC"
    return None


def classify_ownership(operator_type: str | None) -> str:
    value = (operator_type or "").strip().lower()
    if any(token in value for token in PUBLIC_OPERATOR_TOKENS):
        return "PUBLIC"
    return "PRIVATE"


def pick_name(properties: dict[str, Any]) -> tuple[str | None, str]:
    for key in ("name:en", "name_en", "name", "name:ar"):
        value = properties.get(key)
        if isinstance(value, str):
            stripped = value.strip()
            if stripped:
                return stripped, key
    return None, "missing"


def read_csv_rows(path: Path) -> list[dict[str, str]]:
    text = path.read_text(encoding="utf-8-sig")
    return list(csv.DictReader(text.splitlines()))


def read_geojson_features(path: Path) -> list[dict[str, Any]]:
    obj = json.loads(path.read_text(encoding="utf-8"))
    features = obj.get("features", [])
    if not isinstance(features, list):
        return []
    return features


def find_region_id(cur: psycopg.Cursor, lon: float, lat: float) -> int | None:
    cur.execute(
        """
        SELECT region_id
        FROM regions
        WHERE type = 'CAZA'
          AND geometry IS NOT NULL
          AND ST_Intersects(geometry, ST_SetSRID(ST_Point(%s, %s), 4326))
        LIMIT 1
        """,
        (lon, lat),
    )
    row = cur.fetchone()
    return int(row[0]) if row else None


def similarity_report(csv_rows: list[dict[str, str]], features: list[dict[str, Any]]) -> dict[str, int]:
    csv_osm = {str(row.get("osm_id", "")).strip() for row in csv_rows if str(row.get("osm_id", "")).strip()}
    geo_osm = {
        str(feature.get("properties", {}).get("osm_id", "")).strip()
        for feature in features
        if str(feature.get("properties", {}).get("osm_id", "")).strip()
    }
    csv_names = {normalize_name(row.get("loc_name")) for row in csv_rows if normalize_name(row.get("loc_name"))}
    geo_names = {
        normalize_name((feature.get("properties", {}) or {}).get("name:en"))
        or normalize_name((feature.get("properties", {}) or {}).get("name"))
        for feature in features
    }
    geo_names = {name for name in geo_names if name}
    return {
        "csv_rows": len(csv_rows),
        "geo_features": len(features),
        "csv_osm_ids": len(csv_osm),
        "geo_osm_ids": len(geo_osm),
        "shared_osm_ids": len(csv_osm.intersection(geo_osm)),
        "csv_names": len(csv_names),
        "geo_names": len(geo_names),
        "shared_names": len(csv_names.intersection(geo_names)),
    }


def import_facilities(repo_root: Path, csv_path: Path, geojson_path: Path) -> None:
    csv_rows = read_csv_rows(csv_path)
    features = read_geojson_features(geojson_path)
    similarity = similarity_report(csv_rows, features)

    db = db_settings(repo_root)
    with psycopg.connect(**db) as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT COUNT(*) FROM health_facilities")
            before_count = int(cur.fetchone()[0])

            inserted = 0
            skipped_missing_name = 0
            skipped_missing_coords = 0
            skipped_unmapped_type = 0
            skipped_no_region = 0
            skipped_existing = 0
            name_source_counts: dict[str, int] = {"name:en": 0, "name_en": 0, "name": 0, "name:ar": 0, "missing": 0}

            for feature in features:
                properties = feature.get("properties", {}) or {}
                geometry = feature.get("geometry", {}) or {}
                coords = geometry.get("coordinates")
                if geometry.get("type") != "Point" or not isinstance(coords, list) or len(coords) < 2:
                    skipped_missing_coords += 1
                    continue

                lon = coords[0]
                lat = coords[1]
                if lon is None or lat is None:
                    skipped_missing_coords += 1
                    continue

                name, name_source = pick_name(properties)
                name_source_counts[name_source] = name_source_counts.get(name_source, 0) + 1
                if not name:
                    skipped_missing_name += 1
                    continue

                facility_type = classify_facility_type(properties.get("amenity"), properties.get("healthcare"))
                if facility_type is None:
                    skipped_unmapped_type += 1
                    continue

                ownership_type = classify_ownership(properties.get("operator:type"))
                region_id = find_region_id(cur, float(lon), float(lat))
                if region_id is None:
                    skipped_no_region += 1
                    continue

                cur.execute(
                    """
                    SELECT 1
                    FROM health_facilities
                    WHERE LOWER(name) = LOWER(%s)
                      AND latitude = %s
                      AND longitude = %s
                    LIMIT 1
                    """,
                    (name, float(lat), float(lon)),
                )
                if cur.fetchone():
                    skipped_existing += 1
                    continue

                cur.execute(
                    """
                    INSERT INTO health_facilities (
                        region_id, name, facility_type, ownership_type, latitude, longitude, geometry
                    )
                    VALUES (
                        %s, %s, %s, %s, %s, %s, ST_SetSRID(ST_Point(%s, %s), 4326)
                    )
                    """,
                    (region_id, name, facility_type, ownership_type, float(lat), float(lon), float(lon), float(lat)),
                )
                inserted += 1

            conn.commit()

            cur.execute("SELECT COUNT(*) FROM health_facilities")
            after_count = int(cur.fetchone()[0])
            cur.execute(
                """
                SELECT facility_type, COUNT(*)
                FROM health_facilities
                GROUP BY facility_type
                ORDER BY facility_type
                """
            )
            counts_by_type = cur.fetchall()
            cur.execute(
                """
                SELECT COUNT(*)
                FROM health_facilities
                WHERE name ~ '[A-Za-z]'
                """
            )
            latin_name_count = int(cur.fetchone()[0])
            cur.execute(
                """
                SELECT COUNT(*)
                FROM health_facilities
                WHERE name !~ '[A-Za-z]'
                """
            )
            non_latin_name_count = int(cur.fetchone()[0])

    print("=== DATASET SIMILARITY (CSV vs GEOJSON) ===")
    for key, value in similarity.items():
        print(f"{key}={value}")

    print("=== IMPORT SUMMARY ===")
    print(f"before_count={before_count}")
    print(f"inserted={inserted}")
    print(f"after_count={after_count}")
    print(f"skipped_missing_name={skipped_missing_name}")
    print(f"skipped_missing_coords={skipped_missing_coords}")
    print(f"skipped_unmapped_type={skipped_unmapped_type}")
    print(f"skipped_no_region={skipped_no_region}")
    print(f"skipped_existing={skipped_existing}")
    print(f"name_source_counts={name_source_counts}")
    print(f"counts_by_type={counts_by_type}")
    print(f"latin_name_count={latin_name_count}")
    print(f"non_latin_name_count={non_latin_name_count}")


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Import Lebanon health facilities from GeoJSON with CSV similarity checks."
    )
    parser.add_argument("--repo-root", default=str(Path(__file__).resolve().parents[2]))
    parser.add_argument("--csv-path", default="data/healthsites/lebanon_healthsites.csv")
    parser.add_argument("--geojson-path", default="data/healthsites/lebanon_healthsites.geojson")
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve()
    csv_path = (repo_root / args.csv_path).resolve() if not Path(args.csv_path).is_absolute() else Path(args.csv_path)
    geojson_path = (
        (repo_root / args.geojson_path).resolve() if not Path(args.geojson_path).is_absolute() else Path(args.geojson_path)
    )

    import_facilities(repo_root=repo_root, csv_path=csv_path, geojson_path=geojson_path)


if __name__ == "__main__":
    main()
