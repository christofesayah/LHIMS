package com.lhims.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lhims.api.domain.entity.ScenarioAction;

public interface ScenarioActionRepository extends JpaRepository<ScenarioAction, Long> {
    List<ScenarioAction> findByScenarioScenarioId(Long scenarioId);

    void deleteByScenarioScenarioId(Long scenarioId);
}
