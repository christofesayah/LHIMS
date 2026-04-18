import argparse
import json
import os
from pathlib import Path

import psycopg


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


def read_geojson(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def upsert_region(cur, name: str, region_type: str, parent_id: int | None, geometry: dict | None) -> int:
    geometry_json = json.dumps(geometry) if geometry else None
    cur.execute(
        """
        INSERT INTO regions (name, type, parent_region_id, population, geometry)
        VALUES (
            %s,
            %s,
            %s,
            NULL,
            ST_Multi(ST_GeomFromGeoJSON(CAST(%s AS text)))
        )
        ON CONFLICT (name)
        DO UPDATE SET
            type = EXCLUDED.type,
            parent_region_id = EXCLUDED.parent_region_id,
            geometry = EXCLUDED.geometry
        RETURNING region_id
        """,
        (name, region_type, parent_id, geometry_json),
    )
    return int(cur.fetchone()[0])


def main() -> None:
    parser = argparse.ArgumentParser(description="Import Lebanon ADM0/ADM1/ADM2 into regions table.")
    parser.add_argument("--repo-root", default=str(Path(__file__).resolve().parents[2]))
    parser.add_argument(
        "--adm2-geometry-source",
        choices=["simplified", "full"],
        default="simplified",
        help="Use simplified ADM2 geometry for map performance (default) or full geometry.",
    )
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve()
    geo_root = repo_root / "data" / "geo"

    adm0 = read_geojson(geo_root / "adm0" / "lbn_admin0.geojson")
    adm1 = read_geojson(geo_root / "adm1" / "lbn_admin1.geojson")
    adm2_full = read_geojson(geo_root / "adm2" / "lbn_admin2.geojson")
    adm2_simplified = read_geojson(geo_root / "adm2" / "geoBoundaries-LBN-ADM2_simplified.geojson")

    adm2_names = {f["properties"]["adm2_name"] for f in adm2_full["features"]}

    db = db_settings(repo_root)
    with psycopg.connect(**db) as conn:
        with conn.cursor() as cur:
            cur.execute("CREATE EXTENSION IF NOT EXISTS postgis")

            adm0_feature = adm0["features"][0]
            adm0_name = adm0_feature["properties"]["adm0_name"]
            adm0_id = upsert_region(cur, adm0_name, "GOVERNORATE", None, adm0_feature["geometry"])

            adm1_ids: dict[str, int] = {}
            for feature in adm1["features"]:
                raw_name = feature["properties"]["adm1_name"]
                stored_name = raw_name if raw_name not in adm2_names else f"{raw_name} Governorate"
                adm1_id = upsert_region(cur, stored_name, "GOVERNORATE", adm0_id, feature["geometry"])
                adm1_ids[raw_name] = adm1_id

            simplified_by_name = {
                f["properties"]["shapeName"]: f["geometry"] for f in adm2_simplified["features"]
            }
            full_by_name = {f["properties"]["adm2_name"]: f["geometry"] for f in adm2_full["features"]}

            for feature in adm2_full["features"]:
                props = feature["properties"]
                adm2_name = props["adm2_name"]
                adm1_name = props["adm1_name"]
                parent_id = adm1_ids.get(adm1_name)
                geometry = (
                    full_by_name.get(adm2_name)
                    if args.adm2_geometry_source == "full"
                    else simplified_by_name.get(adm2_name, full_by_name.get(adm2_name))
                )
                upsert_region(cur, adm2_name, "CAZA", parent_id, geometry)

            cur.execute("SELECT COUNT(*) FROM regions WHERE type = 'GOVERNORATE'")
            gov_count = int(cur.fetchone()[0])
            cur.execute("SELECT COUNT(*) FROM regions WHERE type = 'CAZA'")
            caza_count = int(cur.fetchone()[0])

        conn.commit()

    print(f"Imported regions successfully. GOVERNORATE={gov_count}, CAZA={caza_count}")


if __name__ == "__main__":
    main()
