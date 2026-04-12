package com.lhims.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "simulation_scenarios")
public class SimulationScenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scenario_id")
    private Long scenarioId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "baseline_period")
    private String baselinePeriod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private UserAccount createdByUser;

    @OneToMany(mappedBy = "scenario")
    private List<IndicatorWeight> indicatorWeights = new ArrayList<>();

    @OneToMany(mappedBy = "scenario")
    private List<ScenarioAction> scenarioActions = new ArrayList<>();

    @OneToMany(mappedBy = "scenario")
    private List<ScenarioResult> scenarioResults = new ArrayList<>();

    @OneToMany(mappedBy = "scenario")
    private List<ComputedScore> computedScores = new ArrayList<>();

    public Long getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(Long scenarioId) {
        this.scenarioId = scenarioId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBaselinePeriod() {
        return baselinePeriod;
    }

    public void setBaselinePeriod(String baselinePeriod) {
        this.baselinePeriod = baselinePeriod;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public UserAccount getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(UserAccount createdByUser) {
        this.createdByUser = createdByUser;
    }

    public List<IndicatorWeight> getIndicatorWeights() {
        return indicatorWeights;
    }

    public void setIndicatorWeights(List<IndicatorWeight> indicatorWeights) {
        this.indicatorWeights = indicatorWeights;
    }

    public List<ScenarioAction> getScenarioActions() {
        return scenarioActions;
    }

    public void setScenarioActions(List<ScenarioAction> scenarioActions) {
        this.scenarioActions = scenarioActions;
    }

    public List<ScenarioResult> getScenarioResults() {
        return scenarioResults;
    }

    public void setScenarioResults(List<ScenarioResult> scenarioResults) {
        this.scenarioResults = scenarioResults;
    }

    public List<ComputedScore> getComputedScores() {
        return computedScores;
    }

    public void setComputedScores(List<ComputedScore> computedScores) {
        this.computedScores = computedScores;
    }

    public SimulationScenario() {
    }
}
