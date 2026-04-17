package com.lhims.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lhims.api.domain.entity.SimulationScenario;

public interface SimulationScenarioRepository extends JpaRepository<SimulationScenario, Long> {
    List<SimulationScenario> findByCreatedByUserUserId(Long userId);

    List<SimulationScenario> findByScenarioActionsRegionRegionId(Long regionId);
}
