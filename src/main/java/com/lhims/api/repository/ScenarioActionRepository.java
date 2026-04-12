package com.lhims.api.repository;

import com.lhims.api.domain.entity.ScenarioAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScenarioActionRepository extends JpaRepository<ScenarioAction, Long> {
    List<ScenarioAction> findByScenarioScenarioId(Long scenarioId);

    void deleteByScenarioScenarioId(Long scenarioId);
}
