package com.lhims.api.web;

import com.lhims.api.security.SecuritySupport;
import com.lhims.api.service.ScenarioService;
import com.lhims.api.web.dto.ScenarioDtos;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/scenarios")
@PreAuthorize("hasAnyRole('MINISTRY_OFFICIAL','HOSPITAL_ADMIN')")
public class ScenarioController {

    private final ScenarioService scenarioService;
    private final SecuritySupport securitySupport;

    public ScenarioController(ScenarioService scenarioService, SecuritySupport securitySupport) {
        this.scenarioService = scenarioService;
        this.securitySupport = securitySupport;
    }

    @GetMapping
    public List<ScenarioDtos.ScenarioResponse> listMine() {
        return scenarioService.listByUser(securitySupport.currentUser().userId());
    }

    @PostMapping
    public ScenarioDtos.ScenarioResponse create(@Valid @RequestBody ScenarioDtos.CreateScenarioRequest request,
                                                HttpServletRequest httpServletRequest) {
        return scenarioService.createScenario(request, securitySupport.currentUser().userId(), httpServletRequest);
    }

    @GetMapping("/{id}")
    public ScenarioDtos.ScenarioResponse one(@PathVariable Long id) {
        return scenarioService.getScenario(id);
    }

    @PostMapping("/{id}/actions")
    public ScenarioDtos.ScenarioActionResponse addAction(@PathVariable Long id,
                                                         @Valid @RequestBody ScenarioDtos.AddScenarioActionRequest request,
                                                         HttpServletRequest httpServletRequest) {
        return scenarioService.addAction(id, request, securitySupport.currentUser().userId(), httpServletRequest);
    }

    @DeleteMapping("/{id}/actions/{actionId}")
    public void removeAction(@PathVariable Long id, @PathVariable Long actionId, HttpServletRequest httpServletRequest) {
        scenarioService.removeAction(id, actionId, securitySupport.currentUser().userId(), httpServletRequest);
    }

    @PostMapping("/{id}/compute")
    public ScenarioDtos.ScenarioResultResponse compute(@PathVariable Long id, HttpServletRequest httpServletRequest) {
        return scenarioService.computeScenario(id, securitySupport.currentUser().userId(), httpServletRequest);
    }

    @GetMapping("/{id}/results")
    public ScenarioDtos.ScenarioResultResponse results(@PathVariable Long id) {
        return scenarioService.getResults(id);
    }

    @GetMapping("/recommended")
    public List<ScenarioDtos.RecommendedScenario> recommended() {
        return scenarioService.getRecommended();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MINISTRY_OFFICIAL')")
    public void delete(@PathVariable Long id, HttpServletRequest httpServletRequest) {
        scenarioService.deleteScenario(id, securitySupport.currentUser().userId(), httpServletRequest);
    }
}
