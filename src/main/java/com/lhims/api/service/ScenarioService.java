package com.lhims.api.service;

import com.lhims.api.domain.entity.ScenarioAction;
import com.lhims.api.domain.entity.ScenarioResult;
import com.lhims.api.domain.entity.SimulationScenario;
import com.lhims.api.domain.enums.AuditActionType;
import com.lhims.api.exception.NotFoundException;
import com.lhims.api.integration.AnalyticsClient;
import com.lhims.api.repository.HealthFacilityRepository;
import com.lhims.api.repository.RegionRepository;
import com.lhims.api.repository.ScenarioActionRepository;
import com.lhims.api.repository.ScenarioResultRepository;
import com.lhims.api.repository.SimulationScenarioRepository;
import com.lhims.api.repository.UserRepository;
import com.lhims.api.web.dto.ScenarioDtos;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ScenarioService {

    private final SimulationScenarioRepository scenarioRepository;
    private final ScenarioActionRepository actionRepository;
    private final ScenarioResultRepository resultRepository;
    private final RegionRepository regionRepository;
    private final HealthFacilityRepository healthFacilityRepository;
    private final UserRepository userRepository;
    private final AnalyticsClient analyticsClient;
    private final AuditService auditService;

    public ScenarioService(SimulationScenarioRepository scenarioRepository,
                           ScenarioActionRepository actionRepository,
                           ScenarioResultRepository resultRepository,
                           RegionRepository regionRepository,
                           HealthFacilityRepository healthFacilityRepository,
                           UserRepository userRepository,
                           AnalyticsClient analyticsClient,
                           AuditService auditService) {
        this.scenarioRepository = scenarioRepository;
        this.actionRepository = actionRepository;
        this.resultRepository = resultRepository;
        this.regionRepository = regionRepository;
        this.healthFacilityRepository = healthFacilityRepository;
        this.userRepository = userRepository;
        this.analyticsClient = analyticsClient;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ScenarioDtos.ScenarioResponse> listByUser(Long userId) {
        return scenarioRepository.findByCreatedByUserUserId(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public ScenarioDtos.ScenarioResponse createScenario(ScenarioDtos.CreateScenarioRequest request, Long userId,
                                                        HttpServletRequest httpServletRequest) {
        // FIX #1: region is now fetched and set on the scenario entity
        var region = regionRepository.findById(request.regionId())
                .orElseThrow(() -> new NotFoundException("Region not found"));

        SimulationScenario scenario = new SimulationScenario(); // FIX: constructor now public
        scenario.setName(request.name());
        scenario.setDescription(request.description());
        scenario.setBaselinePeriod(request.baselinePeriod());
        scenario.setRegion(region); // was missing before
        scenario.setCreatedByUser(userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found")));

        SimulationScenario saved = scenarioRepository.save(scenario);
        auditService.log(
                userId,
                null,
                saved.getRegion() == null ? null : saved.getRegion().getRegionId(),
                AuditActionType.CREATE,
                "simulation_scenarios",
                String.valueOf(saved.getScenarioId()),
                null,
                "CREATE_SCENARIO",
                httpServletRequest
        );
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public ScenarioDtos.ScenarioResponse getScenario(Long id) {
        return toDto(scenarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Scenario not found")));
    }

    @Transactional
    public ScenarioDtos.ScenarioActionResponse addAction(Long scenarioId, ScenarioDtos.AddScenarioActionRequest request,
                                                         Long actorUserId, HttpServletRequest httpServletRequest) {
        SimulationScenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new NotFoundException("Scenario not found"));

        ScenarioAction action = new ScenarioAction(); // FIX: constructor now public
        action.setScenario(scenario);
        action.setRegion(regionRepository.findById(request.regionId())
                .orElseThrow(() -> new NotFoundException("Region not found")));
        action.setActionType(request.actionType());
        action.setOldValue(request.oldValue());
        action.setNewValue(request.newValue());
        if (request.facilityId() != null) {
            action.setFacility(healthFacilityRepository.findById(request.facilityId())
                    .orElseThrow(() -> new NotFoundException("Facility not found")));
        }

        ScenarioAction saved = actionRepository.save(action);
        auditService.log(
                actorUserId,
                null,
                saved.getRegion().getRegionId(),
                AuditActionType.CREATE,
                "scenario_actions",
                String.valueOf(saved.getActionId()),
                null,
                saved.getActionType().name(),
                httpServletRequest
        );
        return toActionDto(saved);
    }

    @Transactional
    public void removeAction(Long scenarioId, Long actionId, Long actorUserId, HttpServletRequest httpServletRequest) {
        ScenarioAction action = actionRepository.findById(actionId)
                .orElseThrow(() -> new NotFoundException("Action not found"));
        if (!action.getScenario().getScenarioId().equals(scenarioId)) {
            throw new NotFoundException("Action not found for scenario");
        }
        Long regionId = action.getRegion() == null ? null : action.getRegion().getRegionId();
        actionRepository.delete(action);
        auditService.log(
                actorUserId,
                null,
                regionId,
                AuditActionType.DELETE,
                "scenario_actions",
                String.valueOf(actionId),
                null,
                "REMOVE_ACTION",
                httpServletRequest
        );
    }

    @Transactional
    public ScenarioDtos.ScenarioResultResponse computeScenario(Long scenarioId, Long actorUserId,
                                                               HttpServletRequest httpServletRequest) {
        SimulationScenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new NotFoundException("Scenario not found"));

        // FIX #2: analytics call is treated as async — we wait for result after it completes,
        // and throw a clear error if the result isn't ready yet instead of returning zeroed-out data
        analyticsClient.computeScenario(scenarioId);

        ScenarioDtos.ScenarioResultResponse response = toResultDto(
                resultRepository.findTopByScenarioScenarioIdOrderByResultIdDesc(scenarioId)
                        .orElseThrow(() -> new NotFoundException(
                                "Computation result not available yet. Please retry shortly."))
        );
        auditService.log(
                actorUserId,
                null,
                scenario.getRegion() == null ? null : scenario.getRegion().getRegionId(),
                AuditActionType.COMPUTE,
                "simulation_scenarios",
                String.valueOf(scenarioId),
                null,
                "COMPUTE_SCENARIO",
                httpServletRequest
        );
        return response;
        // FIX #3: removed the silent zeroed-out fallback that was masking computation failures
    }

    @Transactional(readOnly = true)
    public ScenarioDtos.ScenarioResultResponse getResults(Long scenarioId) {
        return toResultDto(
                resultRepository.findTopByScenarioScenarioIdOrderByResultIdDesc(scenarioId)
                        .orElseThrow(() -> new NotFoundException("Scenario result not found"))
        );
    }

    // FIX #4: removed unnecessary @Transactional — no DB access here
    public List<ScenarioDtos.RecommendedScenario> getRecommended() {
        return analyticsClient.recommended();
    }

    @Transactional
    public void deleteScenario(Long scenarioId, Long actorUserId, HttpServletRequest httpServletRequest) {
        SimulationScenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new NotFoundException("Scenario not found"));

        // FIX #5: delete results first to avoid FK constraint violation
        resultRepository.deleteByScenarioScenarioId(scenarioId);
        actionRepository.deleteByScenarioScenarioId(scenarioId);
        scenarioRepository.delete(scenario);
        auditService.log(
                actorUserId,
                null,
                scenario.getRegion() == null ? null : scenario.getRegion().getRegionId(),
                AuditActionType.DELETE,
                "simulation_scenarios",
                String.valueOf(scenarioId),
                null,
                "DELETE_SCENARIO",
                httpServletRequest
        );
    }

    private ScenarioDtos.ScenarioResponse toDto(SimulationScenario scenario) {
        List<ScenarioDtos.ScenarioActionResponse> actions =
                actionRepository.findByScenarioScenarioId(scenario.getScenarioId()).stream()
                        .map(this::toActionDto)
                        .toList();
        return new ScenarioDtos.ScenarioResponse(
                scenario.getScenarioId(),
                scenario.getName(),
                scenario.getDescription(),
                scenario.getBaselinePeriod(),
                scenario.getCreatedByUser().getUserId(),
                actions
        );
    }

    private ScenarioDtos.ScenarioActionResponse toActionDto(ScenarioAction action) {
        return new ScenarioDtos.ScenarioActionResponse(
                action.getActionId(),
                action.getRegion().getRegionId(),
                action.getFacility() == null ? null : action.getFacility().getFacilityId(),
                action.getActionType(),
                action.getOldValue(),
                action.getNewValue()
        );
    }

    private ScenarioDtos.ScenarioResultResponse toResultDto(ScenarioResult result) {
        return new ScenarioDtos.ScenarioResultResponse(
                result.getScenario().getScenarioId(),
                result.getRegion().getRegionId(),
                result.getSimulatedHai(),
                result.getSimulatedRvi(),
                result.getSimulatedCiri(),
                result.getImpactDelta()
        );
    }
}
