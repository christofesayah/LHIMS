package com.lhims.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lhims.api.domain.entity.ScenarioResult;

public interface ScenarioResultRepository extends JpaRepository<ScenarioResult, Long> {
    Optional<ScenarioResult> findTopByScenarioScenarioIdOrderByResultIdDesc(Long scenarioId);

    void deleteByScenarioScenarioId(Long scenarioId);
}
