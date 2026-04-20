package com.lhims.api.repository;

import com.lhims.api.domain.entity.ComputedScore;
import com.lhims.api.domain.enums.RiskCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComputedScoreRepository extends JpaRepository<ComputedScore, Long> {
    Optional<ComputedScore> findTopByRegionRegionIdAndScenarioIsNullOrderByLastComputedAtDesc(Long regionId);

    List<ComputedScore> findByRegionRegionIdAndScenarioIsNullOrderByLastComputedAtDesc(Long regionId);

    List<ComputedScore> findByScenarioScenarioIdOrderByScoreIdAsc(Long scenarioId);

    List<ComputedScore> findByRiskCategoryAndScenarioIsNull(RiskCategory riskCategory);

    List<ComputedScore> findByScenarioIsNull();
}
