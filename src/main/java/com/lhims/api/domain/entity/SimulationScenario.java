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

    protected SimulationScenario() {
    }
}
