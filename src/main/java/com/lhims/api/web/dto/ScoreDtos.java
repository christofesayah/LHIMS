package com.lhims.api.web.dto;

import com.lhims.api.domain.enums.RiskCategory;

import java.time.LocalDateTime;
import java.util.List;

public final class ScoreDtos {

    private ScoreDtos() {
    }

    public record ScoreResponse(Long regionId, Double haiScore, Double rviScore, Double ciriScore, RiskCategory riskCategory,
                                LocalDateTime lastComputedAt) {
    }

    public record ScoreHistoryResponse(Long regionId, List<ScoreResponse> history) {
    }

    public record KeyInsights(
            ScoreResponse highestRisk,
            ScoreResponse highestVulnerability,
            ScoreResponse lowestAccess,
            Integer totalHighRiskDistricts,
            Integer totalMediumRiskDistricts,
            Integer totalLowRiskDistricts
    ) {
    }
}
