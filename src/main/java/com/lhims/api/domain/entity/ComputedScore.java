package com.lhims.api.domain.entity;

import com.lhims.api.domain.enums.RiskCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "computed_scores")
public class ComputedScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "score_id")
    private Long scoreId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id")
    private SimulationScenario scenario;

    @Column(name = "reporting_period", nullable = false)
    private String reportingPeriod;

    @Column(name = "hai_score")
    private Double haiScore;

    @Column(name = "rvi_score")
    private Double rviScore;

    @Column(name = "ciri_score")
    private Double ciriScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_category")
    private RiskCategory riskCategory;

    @Column(name = "last_computed_at")
    private LocalDateTime lastComputedAt;

    public Long getScoreId() {
        return scoreId;
    }

    public void setScoreId(Long scoreId) {
        this.scoreId = scoreId;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public SimulationScenario getScenario() {
        return scenario;
    }

    public void setScenario(SimulationScenario scenario) {
        this.scenario = scenario;
    }

    public String getReportingPeriod() {
        return reportingPeriod;
    }

    public void setReportingPeriod(String reportingPeriod) {
        this.reportingPeriod = reportingPeriod;
    }

    public Double getHaiScore() {
        return haiScore;
    }

    public void setHaiScore(Double haiScore) {
        this.haiScore = haiScore;
    }

    public Double getRviScore() {
        return rviScore;
    }

    public void setRviScore(Double rviScore) {
        this.rviScore = rviScore;
    }

    public Double getCiriScore() {
        return ciriScore;
    }

    public void setCiriScore(Double ciriScore) {
        this.ciriScore = ciriScore;
    }

    public RiskCategory getRiskCategory() {
        return riskCategory;
    }

    public void setRiskCategory(RiskCategory riskCategory) {
        this.riskCategory = riskCategory;
    }

    public LocalDateTime getLastComputedAt() {
        return lastComputedAt;
    }

    public void setLastComputedAt(LocalDateTime lastComputedAt) {
        this.lastComputedAt = lastComputedAt;
    }

    public ComputedScore() {
    }
}
