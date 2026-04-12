package com.lhims.api.web.dto;

import com.lhims.api.domain.enums.RegionType;
import com.lhims.api.domain.enums.RiskCategory;

import java.util.List;

public final class RegionDtos {

    private RegionDtos() {
    }

    public record DistrictSummary(Long id, String name, RiskCategory riskCategory, Double ciriScore) {
    }

    public record DistrictDetail(Long id, String name, RegionType type, Integer population, RiskCategory riskCategory, Double haiScore,
                                 Double rviScore, Double ciriScore) {
    }

    public record DistrictCompare(DistrictDetail first, DistrictDetail second) {
    }

    public record DistrictGeoJsonFeature(Long id, String name, String geoJson) {
    }

    public record DistrictGeoJsonResponse(List<DistrictGeoJsonFeature> features) {
    }
}
