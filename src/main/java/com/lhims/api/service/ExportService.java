package com.lhims.api.service;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lhims.api.web.dto.ScenarioDtos;
import com.lhims.api.web.dto.ScoreDtos;

@Service
public class ExportService {

    private final ScoreService scoreService;
    private final ScenarioService scenarioService;

    public ExportService(ScoreService scoreService, ScenarioService scenarioService) {
        this.scoreService = scoreService;
        this.scenarioService = scenarioService;
    }

    @Transactional(readOnly = true)
    public byte[] districtPdf(Long districtId) {
        ScoreDtos.ScoreResponse score = scoreService.getScores(districtId);
        String body = "LHIMS District Report\nRegion: " + districtId + "\nHAI: " + score.haiScore()
                + "\nRVI: " + score.rviScore() + "\nCIRI: " + score.ciriScore();
        return body.getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] districtsCsv() {
        List<ScoreDtos.ScoreResponse> all = scoreService.getAllScores();
        StringBuilder sb = new StringBuilder("region_id,hai,rvi,ciri,risk,last_computed_at\n");
        for (ScoreDtos.ScoreResponse s : all) {
            sb.append(s.regionId()).append(',')
                    .append(s.haiScore()).append(',')
                    .append(s.rviScore()).append(',')
                    .append(s.ciriScore()).append(',')
                    .append(s.riskCategory()).append(',')
                    .append(s.lastComputedAt())
                    .append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] scenarioPdf(Long scenarioId) {
        ScenarioDtos.ScenarioResultResponse result = scenarioService.getResults(scenarioId);
        String body = "LHIMS Scenario Report\nScenario: " + scenarioId + "\nRegion: " + result.regionId()
                + "\nSimulated HAI: " + result.simulatedHai()
                + "\nSimulated RVI: " + result.simulatedRvi()
                + "\nSimulated CIRI: " + result.simulatedCiri()
                + "\nDelta: " + result.impactDelta();
        return body.getBytes(StandardCharsets.UTF_8);
    }
}
