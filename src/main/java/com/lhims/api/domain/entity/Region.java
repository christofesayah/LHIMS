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

    protected Region() {
    }
}
