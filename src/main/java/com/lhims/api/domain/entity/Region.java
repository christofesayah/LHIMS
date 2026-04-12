package com.lhims.api.domain.entity;

import com.lhims.api.domain.enums.RegionType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.MultiPolygon;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "regions")
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "region_id")
    private Long regionId;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private RegionType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_region_id")
    private Region parentRegion;

    @Column(name = "population")
    private Integer population;

    @Column(name = "geometry", columnDefinition = "geometry(MultiPolygon, 4326)")
    private MultiPolygon geometry;

    @OneToMany(mappedBy = "parentRegion")
    private List<Region> childRegions = new ArrayList<>();

    @OneToMany(mappedBy = "region")
    private List<HealthFacility> healthFacilities = new ArrayList<>();

    @OneToMany(mappedBy = "region")
    private List<RegionalVulnerability> vulnerabilities = new ArrayList<>();

    @OneToMany(mappedBy = "region")
    private List<RegionalAccessMetric> accessMetrics = new ArrayList<>();

    @OneToMany(mappedBy = "region")
    private List<ComputedScore> computedScores = new ArrayList<>();

    @OneToMany(mappedBy = "region")
    private List<ScenarioAction> scenarioActions = new ArrayList<>();

    @OneToMany(mappedBy = "region")
    private List<ScenarioResult> scenarioResults = new ArrayList<>();

    public Long getRegionId() {
        return regionId;
    }

    public void setRegionId(Long regionId) {
        this.regionId = regionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RegionType getType() {
        return type;
    }

    public void setType(RegionType type) {
        this.type = type;
    }

    public Region getParentRegion() {
        return parentRegion;
    }

    public void setParentRegion(Region parentRegion) {
        this.parentRegion = parentRegion;
    }

    public Integer getPopulation() {
        return population;
    }

    public void setPopulation(Integer population) {
        this.population = population;
    }

    public MultiPolygon getGeometry() {
        return geometry;
    }

    public void setGeometry(MultiPolygon geometry) {
        this.geometry = geometry;
    }

    public List<Region> getChildRegions() {
        return childRegions;
    }

    public void setChildRegions(List<Region> childRegions) {
        this.childRegions = childRegions;
    }

    public List<HealthFacility> getHealthFacilities() {
        return healthFacilities;
    }

    public void setHealthFacilities(List<HealthFacility> healthFacilities) {
        this.healthFacilities = healthFacilities;
    }

    public List<RegionalVulnerability> getVulnerabilities() {
        return vulnerabilities;
    }

    public void setVulnerabilities(List<RegionalVulnerability> vulnerabilities) {
        this.vulnerabilities = vulnerabilities;
    }

    public List<RegionalAccessMetric> getAccessMetrics() {
        return accessMetrics;
    }

    public void setAccessMetrics(List<RegionalAccessMetric> accessMetrics) {
        this.accessMetrics = accessMetrics;
    }

    public List<ComputedScore> getComputedScores() {
        return computedScores;
    }

    public void setComputedScores(List<ComputedScore> computedScores) {
        this.computedScores = computedScores;
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

    protected Region() {
    }
}
