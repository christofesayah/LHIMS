package com.lhims.api.web;

import com.lhims.api.security.SecuritySupport;
import com.lhims.api.service.ScoreService;
import com.lhims.api.web.dto.ScoreDtos;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/districts")
public class ScoreController {

    private final ScoreService scoreService;
    private final SecuritySupport securitySupport;

    public ScoreController(ScoreService scoreService, SecuritySupport securitySupport) {
        this.scoreService = scoreService;
        this.securitySupport = securitySupport;
    }

    @GetMapping("/{id}/scores")
    public ScoreDtos.ScoreResponse getScores(@PathVariable Long id) {
        return scoreService.getScores(id);
    }

    @GetMapping("/scores/all")
    public List<ScoreDtos.ScoreResponse> allScores() {
        return scoreService.getAllScores();
    }

    @PostMapping("/{id}/compute")
    @PreAuthorize("hasAnyRole('MINISTRY_OFFICIAL','HOSPITAL_ADMIN')")
    public ScoreDtos.ScoreResponse compute(@PathVariable Long id, HttpServletRequest httpServletRequest) {
        return scoreService.computeScores(id, securitySupport.currentUser().userId(), httpServletRequest);
    }

    @GetMapping("/{id}/scores/history")
    @PreAuthorize("hasRole('MINISTRY_OFFICIAL')")
    public ScoreDtos.ScoreHistoryResponse history(@PathVariable Long id) {
        return scoreService.history(id);
    }

    @GetMapping("/scores/high-risk")
    @PreAuthorize("hasAnyRole('MINISTRY_OFFICIAL','HOSPITAL_ADMIN')")
    public List<ScoreDtos.ScoreResponse> highRisk() {
        return scoreService.highRiskDistricts();
    }

    @GetMapping("/scores/insights")
    public ScoreDtos.KeyInsights getInsights() {
        return scoreService.getInsights();
    }
}
