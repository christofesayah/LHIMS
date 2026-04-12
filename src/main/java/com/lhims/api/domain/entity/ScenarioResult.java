package com.lhims.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "scenario_results")
public class ScenarioResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long resultId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private SimulationScenario scenario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(name = "simulated_hai")
    private Double simulatedHai;

    @Column(name = "simulated_rvi")
    private Double simulatedRvi;

    @Column(name = "simulated_ciri")
    private Double simulatedCiri;

    @Column(name = "impact_delta")
    private Double impactDelta;

    public Long getResultId() {
        return resultId;
    }

    public void setResultId(Long resultId) {
        this.resultId = resultId;
    }

    public SimulationScenario getScenario() {
        return scenario;
    }

    public void setScenario(SimulationScenario scenario) {
        this.scenario = scenario;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public Double getSimulatedHai() {
        return simulatedHai;
    }

    public void setSimulatedHai(Double simulatedHai) {
        this.simulatedHai = simulatedHai;
    }

    public Double getSimulatedRvi() {
        return simulatedRvi;
    }

    public void setSimulatedRvi(Double simulatedRvi) {
        this.simulatedRvi = simulatedRvi;
    }

    public Double getSimulatedCiri() {
        return simulatedCiri;
    }

    public void setSimulatedCiri(Double simulatedCiri) {
        this.simulatedCiri = simulatedCiri;
    }

    public Double getImpactDelta() {
        return impactDelta;
    }

    public void setImpactDelta(Double impactDelta) {
        this.impactDelta = impactDelta;
    }

    protected ScenarioResult() {
    }
}
