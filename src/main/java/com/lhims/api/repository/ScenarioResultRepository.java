package com.lhims.api.repository;

import com.lhims.api.domain.entity.ScenarioResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScenarioResultRepository extends JpaRepository<ScenarioResult, Long> {
    Optional<ScenarioResult> findTopByScenarioScenarioIdOrderByResultIdDesc(Long scenarioId);

    void deleteByScenarioScenarioId(Long scenarioId);
}
