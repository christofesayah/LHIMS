package com.lhims.api.repository;

import com.lhims.api.domain.entity.SimulationScenario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SimulationScenarioRepository extends JpaRepository<SimulationScenario, Long> {
    List<SimulationScenario> findByCreatedByUserUserId(Long userId);

    List<SimulationScenario> findByScenarioActionsRegionRegionId(Long regionId);
}
