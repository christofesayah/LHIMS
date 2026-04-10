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

    protected ComputedScore() {
    }
}
