package com.lhims.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "indicator_definitions")
public class IndicatorDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "indicator_id")
    private Long indicatorId;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "indicator_name", nullable = false)
    private String indicatorName;

    @Column(name = "formula_expression", columnDefinition = "text")
    private String formulaExpression;

    @Column(name = "normalization_method")
    private String normalizationMethod;

    @Column(name = "weight")
    private Double weight;

    @Column(name = "normalization_min")
    private Double normalizationMin;

    @Column(name = "normalization_max")
    private Double normalizationMax;

    @OneToMany(mappedBy = "indicator")
    private List<IndicatorWeight> indicatorWeights = new ArrayList<>();

    protected IndicatorDefinition() {
    }
}
