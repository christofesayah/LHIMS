package com.lhims.api.integration;

import com.lhims.api.web.dto.ScenarioDtos;

import java.util.List;

public interface AnalyticsClient {
    void computeRegion(Long regionId);

    void computeScenario(Long scenarioId);

    List<ScenarioDtos.RecommendedScenario> recommended();
}
