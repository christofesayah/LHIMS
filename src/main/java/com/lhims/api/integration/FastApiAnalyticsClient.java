package com.lhims.api.integration;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.lhims.api.web.dto.ScenarioDtos;

@Component
public class FastApiAnalyticsClient implements AnalyticsClient {

    private final RestTemplate restTemplate;
    private final String analyticsBaseUrl;

    public FastApiAnalyticsClient(RestTemplate restTemplate,
                                  @Value("${app.analytics.base-url:http://localhost:8000}") String analyticsBaseUrl) {
        this.restTemplate = restTemplate;
        this.analyticsBaseUrl = analyticsBaseUrl;
    }

    @Override
    public void computeRegion(Long regionId) {
        try {
            restTemplate.postForObject(analyticsBaseUrl + "/compute/" + regionId, null, Void.class);
        } catch (RestClientException ignored) {
            // Allow DB-only fallback behavior when Python is not reachable.
        }
    }

    @Override
    public void computeScenario(Long scenarioId) {
        try {
            restTemplate.postForObject(analyticsBaseUrl + "/compute/scenario/" + scenarioId, null, Void.class);
        } catch (RestClientException ignored) {
            // Allow DB-only fallback behavior when Python is not reachable.
        }
    }

    @Override
    public List<ScenarioDtos.RecommendedScenario> recommended() {
        try {
            ResponseEntity<List<ScenarioDtos.RecommendedScenario>> response = restTemplate.exchange(
                    analyticsBaseUrl + "/recommend",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );
            return response.getBody() == null ? Collections.emptyList() : response.getBody();
        } catch (RestClientException ignored) {
            return Collections.emptyList();
        }
    }
}
