import os
from datetime import datetime
from typing import Any

import numpy as np
import psycopg
from fastapi import FastAPI, HTTPException

app = FastAPI(title="LHIMS Analytics Service", version="1.0.0")

DEFAULT_P33 = -0.14
DEFAULT_P67 = 0.29

DISTRICT_NAME_ALIASES = {
    "minieh danniyeh": "Minieh-Danniyeh",
    "minieh-danniyeh": "Minieh-Danniyeh",
    "beqaa west": "West Bekaa",
    "west beqaa": "West Bekaa",
    "nabatiyeh": "Nabatieh",
}


def db_connection() -> psycopg.Connection:
    return psycopg.connect(
        host=os.getenv("DB_HOST", "localhost"),
        port=int(os.getenv("DB_PORT", "5400")),
        dbname=os.getenv("DB_NAME", "lhims_db"),
        user=os.getenv("DB_USER", "lhims_user"),
        password=os.getenv("DB_PASS", "lhims_pass"),
    )


def classify(ciri: float, p33: float, p67: float) -> str:
    if ciri <= p33:
        return "LOW"
    if ciri <= p67:
        return "MEDIUM"
    return "HIGH"


def clamp(value: float, minimum: float = 0.0, maximum: float = 1.0) -> float:
    return float(np.clip(value, minimum, maximum))


def standardize_district_name(name: str | None) -> str:
    raw = (name or "").strip()
    compact = " ".join(raw.replace("-", " ").split()).lower()
    if compact in DISTRICT_NAME_ALIASES:
        return DISTRICT_NAME_ALIASES[compact]
    return " ".join(token.capitalize() for token in raw.replace("-", " ").split())


def to_per_capita(value: float | None, population: float) -> float:
    if value is None or population <= 0:
        return np.nan
    val = float(value)
    if not np.isfinite(val) or val < 0:
        return np.nan
    if val <= 1:
        return val
    return val / float(population)


def to_rate(value: float | None) -> float:
    if value is None:
        return np.nan
    val = float(value)
    if not np.isfinite(val) or val < 0:
        return np.nan
    if val > 1 and val <= 100:
        val = val / 100.0
    return clamp(val, 0.0, 1.0)


def min_max_normalize(values: np.ndarray) -> np.ndarray:
    out = np.full(values.shape, np.nan, dtype=float)
    finite = np.isfinite(values)
    if not np.any(finite):
        return out
    finite_vals = values[finite]
    vmin = float(np.min(finite_vals))
    vmax = float(np.max(finite_vals))
    if np.isclose(vmax, vmin):
        out[finite] = 0.5
        return out
    out[finite] = (finite_vals - vmin) / (vmax - vmin)
    return out


def nanmean_or_zero(values: np.ndarray) -> float:
    finite = values[np.isfinite(values)]
    if finite.size == 0:
        return 0.0
    return float(np.mean(finite))


def build_scores(conn: psycopg.Connection) -> tuple[dict[int, dict[str, Any]], float, float, dict[int, str]]:
    with conn.cursor() as cur:
        cur.execute("SELECT region_id, name, COALESCE(population, 0) FROM regions")
        region_rows = cur.fetchall()
        if not region_rows:
            return {}, DEFAULT_P33, DEFAULT_P67, {}

        cur.execute("SELECT facility_id, region_id, facility_type FROM health_facilities")
        facility_rows = cur.fetchall()

        cur.execute(
            """
            SELECT hf.region_id, fc.facility_id, fc.reporting_date, fc.doctors_count, fc.nurses_count
            FROM facility_capacity fc
            JOIN health_facilities hf ON hf.facility_id = fc.facility_id
            """
        )
        capacity_rows = cur.fetchall()

        cur.execute("SELECT region_id, poverty_rate, demographic_pressure FROM regional_vulnerability")
        vulnerability_rows = cur.fetchall()

    populations: dict[int, float] = {}
    district_names: dict[int, str] = {}
    for region_id, region_name, population in region_rows:
        region_key = int(region_id)
        district_names[region_key] = standardize_district_name(region_name)
        populations[region_key] = float(population or 0)

    phc_ids: dict[int, set[int]] = {region_id: set() for region_id in populations}
    hospital_ids: dict[int, set[int]] = {region_id: set() for region_id in populations}
    for facility_id, region_id, facility_type in facility_rows:
        region_key = int(region_id)
        if region_key not in populations:
            continue
        if facility_type == "PHC":
            phc_ids[region_key].add(int(facility_id))
        elif facility_type == "HOSPITAL":
            hospital_ids[region_key].add(int(facility_id))

    latest_by_facility: dict[tuple[int, int], tuple[Any, float]] = {}
    for region_id, facility_id, reporting_date, doctors_count, nurses_count in capacity_rows:
        region_key = int(region_id)
        facility_key = int(facility_id)
        doctors = float(doctors_count or 0)
        nurses = float(nurses_count or 0)
        if doctors < 0 or nurses < 0:
            continue
        workforce = doctors + nurses
        key = (region_key, facility_key)
        previous = latest_by_facility.get(key)
        if previous is None or (reporting_date is not None and reporting_date > previous[0]):
            latest_by_facility[key] = (reporting_date, workforce)

    workforce_counts: dict[int, float] = {region_id: 0.0 for region_id in populations}
    for (region_id, _), (_, workforce) in latest_by_facility.items():
        workforce_counts[region_id] = workforce_counts.get(region_id, 0.0) + workforce

    poverty_samples: dict[int, list[float]] = {region_id: [] for region_id in populations}
    density_samples: dict[int, list[float]] = {region_id: [] for region_id in populations}
    seen_vuln_rows: set[tuple[int, float, float]] = set()
    for region_id, poverty_rate, demographic_pressure in vulnerability_rows:
        region_key = int(region_id)
        if region_key not in populations:
            continue
        pov = to_rate(poverty_rate)
        den = to_per_capita(demographic_pressure, populations[region_key])
        dedupe_key = (
            region_key,
            float(np.nan_to_num(pov, nan=-1.0)),
            float(np.nan_to_num(den, nan=-1.0)),
        )
        if dedupe_key in seen_vuln_rows:
            continue
        seen_vuln_rows.add(dedupe_key)
        if np.isfinite(pov):
            poverty_samples[region_key].append(pov)
        if np.isfinite(den):
            density_samples[region_key].append(den)

    region_ids = list(populations.keys())
    phc_pc = np.array(
        [to_per_capita(len(phc_ids.get(region_id, set())), populations[region_id]) for region_id in region_ids],
        dtype=float,
    )
    hosp_pc = np.array(
        [to_per_capita(len(hospital_ids.get(region_id, set())), populations[region_id]) for region_id in region_ids],
        dtype=float,
    )
    workforce_pc = np.array(
        [to_per_capita(workforce_counts.get(region_id, 0.0), populations[region_id]) for region_id in region_ids],
        dtype=float,
    )
    poverty = np.array(
        [
            nanmean_or_zero(np.array(poverty_samples.get(region_id, []), dtype=float))
            if poverty_samples.get(region_id)
            else np.nan
            for region_id in region_ids
        ],
        dtype=float,
    )
    density = np.array(
        [
            nanmean_or_zero(np.array(density_samples.get(region_id, []), dtype=float))
            if density_samples.get(region_id)
            else np.nan
            for region_id in region_ids
        ],
        dtype=float,
    )

    phc_norm = min_max_normalize(phc_pc)
    hosp_norm = min_max_normalize(hosp_pc)
    workforce_norm = min_max_normalize(workforce_pc)
    poverty_norm = min_max_normalize(poverty)
    density_norm = min_max_normalize(density)

    all_scores: dict[int, dict[str, Any]] = {}
    ciri_values: list[float] = []

    for idx, region_id in enumerate(region_ids):
        hai = nanmean_or_zero(np.array([phc_norm[idx], hosp_norm[idx], workforce_norm[idx]], dtype=float))
        # Refugee indicator intentionally excluded per project update.
        rvi = nanmean_or_zero(np.array([poverty_norm[idx], density_norm[idx]], dtype=float))
        ciri = float(rvi - hai)
        all_scores[region_id] = {
            "haiScore": round(float(np.clip(hai, 0.0, 1.0)), 4),
            "rviScore": round(float(np.clip(rvi, 0.0, 1.0)), 4),
            "ciriScore": round(ciri, 4),
        }
        ciri_values.append(ciri)

    ciri_array = np.array(ciri_values, dtype=float)
    finite_ciri = ciri_array[np.isfinite(ciri_array)]
    if finite_ciri.size >= 3:
        p33 = float(np.percentile(finite_ciri, 33))
        p67 = float(np.percentile(finite_ciri, 67))
    else:
        p33 = DEFAULT_P33
        p67 = DEFAULT_P67

    for region_id, score in all_scores.items():
        score["riskCategory"] = classify(score["ciriScore"], p33, p67)

    return all_scores, p33, p67, district_names


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/compute/{region_id}")
def compute_region(region_id: int) -> dict[str, Any]:
    with db_connection() as conn:
        all_scores, p33, p67, district_names = build_scores(conn)
        if region_id not in all_scores:
            raise HTTPException(status_code=404, detail="Region not found")

        with conn.cursor() as cur:
            for rid, district_name in district_names.items():
                cur.execute("UPDATE regions SET name = %s WHERE region_id = %s AND name <> %s", (district_name, rid, district_name))

            score = all_scores[region_id]

            cur.execute(
                """
                INSERT INTO computed_scores (
                    region_id, scenario_id, reporting_period, hai_score, rvi_score, ciri_score, risk_category, last_computed_at
                ) VALUES (%s, NULL, %s, %s, %s, %s, %s, %s)
                RETURNING score_id
                """,
                (
                    region_id,
                    "latest",
                    score["haiScore"],
                    score["rviScore"],
                    score["ciriScore"],
                    score["riskCategory"],
                    datetime.utcnow(),
                ),
            )
            score_id = cur.fetchone()[0]
            conn.commit()

            return {
                "scoreId": score_id,
                "regionId": region_id,
                "haiScore": score["haiScore"],
                "rviScore": score["rviScore"],
                "ciriScore": score["ciriScore"],
                "riskCategory": score["riskCategory"],
                "p33": round(p33, 4),
                "p67": round(p67, 4),
            }


@app.post("/compute/scenario/{scenario_id}")
def compute_scenario(scenario_id: int) -> dict[str, Any]:
    with db_connection() as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT region_id FROM simulation_scenarios WHERE scenario_id = %s", (scenario_id,))
            row = cur.fetchone()
            if row is None:
                raise HTTPException(status_code=404, detail="Scenario not found")

            region_id = row[0]
            if region_id is None:
                cur.execute(
                    "SELECT region_id FROM scenario_actions WHERE scenario_id = %s ORDER BY action_id LIMIT 1",
                    (scenario_id,),
                )
                action_region = cur.fetchone()
                region_id = action_region[0] if action_region else None
            if region_id is None:
                raise HTTPException(status_code=400, detail="Scenario has no region context")

            cur.execute(
                """
                SELECT hai_score, rvi_score
                FROM computed_scores
                WHERE region_id = %s AND scenario_id IS NULL
                ORDER BY last_computed_at DESC
                LIMIT 1
                """,
                (region_id,),
            )
            base = cur.fetchone()
            base_hai = float(base[0]) if base and base[0] is not None else 0.5
            base_rvi = float(base[1]) if base and base[1] is not None else 0.5

            cur.execute(
                "SELECT old_value, new_value FROM scenario_actions WHERE scenario_id = %s",
                (scenario_id,),
            )
            actions = cur.fetchall()

            numeric_delta = 0.0
            for old_value, new_value in actions:
                try:
                    old_num = float(old_value) if old_value is not None else 0.0
                    new_num = float(new_value) if new_value is not None else 0.0
                    numeric_delta += new_num - old_num
                except (TypeError, ValueError):
                    continue

            if numeric_delta == 0.0 and actions:
                numeric_delta = float(len(actions)) * 0.05

            simulated_hai = round(clamp(base_hai + (numeric_delta * 0.1)), 4)
            simulated_rvi = round(clamp(base_rvi - (numeric_delta * 0.1)), 4)
            simulated_ciri = round(simulated_rvi - simulated_hai, 4)
            impact_delta = round(simulated_ciri - (base_rvi - base_hai), 4)

            cur.execute(
                """
                INSERT INTO scenario_results (
                    scenario_id, region_id, simulated_hai, simulated_rvi, simulated_ciri, impact_delta
                ) VALUES (%s, %s, %s, %s, %s, %s)
                RETURNING result_id
                """,
                (scenario_id, region_id, simulated_hai, simulated_rvi, simulated_ciri, impact_delta),
            )
            result_id = cur.fetchone()[0]
            conn.commit()

            return {
                "resultId": result_id,
                "scenarioId": scenario_id,
                "regionId": region_id,
                "simulatedHai": simulated_hai,
                "simulatedRvi": simulated_rvi,
                "simulatedCiri": simulated_ciri,
                "impactDelta": impact_delta,
            }


@app.get("/recommend")
def recommend() -> list[dict[str, Any]]:
    with db_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT cs.region_id, r.name, cs.hai_score, cs.rvi_score
                FROM computed_scores cs
                JOIN regions r ON r.region_id = cs.region_id
                JOIN (
                    SELECT region_id, MAX(last_computed_at) AS max_time
                    FROM computed_scores
                    WHERE scenario_id IS NULL
                    GROUP BY region_id
                ) latest ON latest.region_id = cs.region_id AND latest.max_time = cs.last_computed_at
                WHERE cs.scenario_id IS NULL AND cs.risk_category = 'HIGH'
                ORDER BY cs.ciri_score DESC NULLS LAST
                """
            )
            rows = cur.fetchall()

    recommendations: list[dict[str, Any]] = []
    for region_id, district_name, hai_score, rvi_score in rows:
        district_clean = standardize_district_name(district_name)
        actions: list[str] = []
        if hai_score is not None and float(hai_score) < 0.4:
            actions.append("Increase primary healthcare capacity")
        if rvi_score is not None and float(rvi_score) > 0.6:
            actions.append("Scale social vulnerability interventions")
        if not actions:
            actions.append("Maintain active monitoring and targeted response")
        recommendations.append(
            {
                "regionId": region_id,
                "districtName": district_clean,
                "actions": actions,
            }
        )
    return recommendations
