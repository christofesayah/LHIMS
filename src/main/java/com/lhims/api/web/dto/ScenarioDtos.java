package com.lhims.api.web.dto;

import com.lhims.api.domain.enums.ScenarioActionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class ScenarioDtos {

    private ScenarioDtos() {
    }

    public record CreateScenarioRequest(@NotBlank String name, String description, String baselinePeriod, @NotNull Long regionId) {
    }

    public record AddScenarioActionRequest(@NotNull Long regionId, Long facilityId, @NotNull ScenarioActionType actionType,
                                           String oldValue, String newValue) {
    }

    public record ScenarioResponse(Long scenarioId, String name, String description, String baselinePeriod, Long createdByUserId,
                                   List<ScenarioActionResponse> actions) {
    }

    public record ScenarioActionResponse(Long actionId, Long regionId, Long facilityId, ScenarioActionType actionType,
                                         String oldValue, String newValue) {
    }

    public record ScenarioResultResponse(Long scenarioId, Long regionId, Double simulatedHai, Double simulatedRvi,
                                         Double simulatedCiri, Double impactDelta) {
    }

    public record RecommendedScenario(Long regionId, String districtName, List<String> actions) {
    }
}
