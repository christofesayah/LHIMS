import argparse
import csv
import json
import os
from pathlib import Path

import psycopg


REPORTING_PERIOD = "LBN_ACCESS_TIME_7200"


NAME_ALIASES = {
    "Batroun": "El Batroun",
    "Bent Jbail": "Bent Jbeil",
    "El Metn": "El Meten",
    "Hermel": "El Hermel",
    "Jbail": "Jbeil",
    "Koura": "El Koura",
    "Minieh-Dinnieh": "El Minieh-Dennie",
    "Nabatiye": "El Nabatieh",
    "Kesrouan": "Kesrwane",
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


def canonical_name(name: str) -> str:
    return NAME_ALIASES.get(name.strip(), name.strip())


def read_adm2_name_by_pcode(path: Path) -> dict[str, str]:
    geojson = json.loads(path.read_text(encoding="utf-8"))
    mapping: dict[str, str] = {}
    for feature in geojson["features"]:
        props = feature["properties"]
        mapping[props["adm2_pcode"]] = props["adm2_name"]
    return mapping


def read_hospital_counts(path: Path, pcode_to_name: dict[str, str]) -> dict[str, int]:
    rows = csv.DictReader(path.read_text(encoding="utf-8-sig").splitlines())
    counts_by_name: dict[str, int] = {}
    for row in rows:
        pcode = row["ADM2_PCODE"].strip()
        district_name = pcode_to_name.get(pcode)
        if district_name is None:
            continue
        counts_by_name[district_name] = int(row["hospitals_count"])
    return counts_by_name


def read_population_at_max_range(path: Path) -> dict[str, int]:
    rows = csv.DictReader(path.read_text(encoding="utf-8-sig").splitlines())
    max_range_by_name: dict[str, tuple[int, int]] = {}
    for row in rows:
        if row["admin_level"] != "ADM2":
            continue
        if row["population_type"] != "total":
            continue
        if row["range_type"] != "TIME":
            continue
        name = canonical_name(row["name"])
        range_value = int(float(row["range"]))
        population = int(float(row["population"]))
        previous = max_range_by_name.get(name)
        if previous is None or range_value > previous[0]:
            max_range_by_name[name] = (range_value, population)
    return {name: values[1] for name, values in max_range_by_name.items()}


def main() -> None:
    parser = argparse.ArgumentParser(description="Populate regions.population and regional_access_metrics from CSV files.")
    parser.add_argument("--repo-root", default=str(Path(__file__).resolve().parents[2]))
    parser.add_argument("--metrics-csv", default="data/metrics/LBN_ADM2_facilities.csv")
    parser.add_argument("--access-csv", default="data/access/LBN_hospitals_access_long.csv")
    parser.add_argument("--adm2-geojson", default="data/geo/adm2/lbn_admin2.geojson")
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve()
    metrics_csv = (repo_root / args.metrics_csv).resolve()
    access_csv = (repo_root / args.access_csv).resolve()
    adm2_geojson = (repo_root / args.adm2_geojson).resolve()

    pcode_to_name = read_adm2_name_by_pcode(adm2_geojson)
    hospital_counts = read_hospital_counts(metrics_csv, pcode_to_name)
    population_by_name = read_population_at_max_range(access_csv)

    db = db_settings(repo_root)
    with psycopg.connect(**db) as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT COUNT(*) FROM regions WHERE type = 'CAZA' AND population IS NOT NULL")
            before_populated_regions = int(cur.fetchone()[0])
            cur.execute("SELECT COUNT(*) FROM regional_access_metrics")
            before_metrics_count = int(cur.fetchone()[0])

            cur.execute("DELETE FROM regional_access_metrics WHERE reporting_period = %s", (REPORTING_PERIOD,))

            population_updates = 0
            metric_inserts = 0
            skipped_missing_population = 0
            skipped_missing_hospital_count = 0
            skipped_missing_region = 0

            for district_name, population in population_by_name.items():
                if population <= 0:
                    skipped_missing_population += 1
                    continue

                cur.execute(
                    """
                    SELECT region_id
                    FROM regions
                    WHERE type = 'CAZA' AND name = %s
                    """,
                    (district_name,),
                )
                region_row = cur.fetchone()
                if not region_row:
                    skipped_missing_region += 1
                    continue

                region_id = int(region_row[0])
                cur.execute("UPDATE regions SET population = %s WHERE region_id = %s", (population, region_id))
                population_updates += 1

                hospital_count = hospital_counts.get(district_name)
                if hospital_count is None:
                    skipped_missing_hospital_count += 1
                    continue

                hospitals_per_capita = float(hospital_count) / float(population)
                cur.execute(
                    """
                    INSERT INTO regional_access_metrics (
                        region_id, reporting_period, hospitals_per_capita, doctors_per_capita, icu_beds_per_capita
                    )
                    VALUES (%s, %s, %s, NULL, NULL)
                    """,
                    (region_id, REPORTING_PERIOD, hospitals_per_capita),
                )
                metric_inserts += 1

            conn.commit()

            cur.execute("SELECT COUNT(*) FROM regions WHERE type = 'CAZA' AND population IS NOT NULL")
            after_populated_regions = int(cur.fetchone()[0])
            cur.execute("SELECT COUNT(*) FROM regional_access_metrics")
            after_metrics_count = int(cur.fetchone()[0])
            cur.execute(
                """
                SELECT reporting_period, COUNT(*)
                FROM regional_access_metrics
                WHERE reporting_period = %s
                GROUP BY reporting_period
                """,
                (REPORTING_PERIOD,),
            )
            inserted_rows = cur.fetchall()

    print("=== IMPORT SUMMARY ===")
    print(f"reporting_period={REPORTING_PERIOD}")
    print(f"regions_population_before={before_populated_regions}")
    print(f"regions_population_after={after_populated_regions}")
    print(f"regions_population_updated={population_updates}")
    print(f"regional_access_metrics_before={before_metrics_count}")
    print(f"regional_access_metrics_after={after_metrics_count}")
    print(f"regional_access_metrics_inserted={metric_inserts}")
    print(f"inserted_rows={inserted_rows}")
    print(f"skipped_missing_population={skipped_missing_population}")
    print(f"skipped_missing_hospital_count={skipped_missing_hospital_count}")
    print(f"skipped_missing_region={skipped_missing_region}")


if __name__ == "__main__":
    main()
