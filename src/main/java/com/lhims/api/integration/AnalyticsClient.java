package com.lhims.api.integration;

import java.util.List;

import com.lhims.api.web.dto.ScenarioDtos;

public interface AnalyticsClient {
    void computeRegion(Long regionId);

    void computeScenario(Long scenarioId);

    List<ScenarioDtos.RecommendedScenario> recommended();
}
