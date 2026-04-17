package com.lhims.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lhims.api.domain.entity.HealthFacility;
import com.lhims.api.domain.enums.FacilityType;

public interface HealthFacilityRepository extends JpaRepository<HealthFacility, Long> {
    List<HealthFacility> findByRegionRegionId(Long regionId);

    List<HealthFacility> findByFacilityType(FacilityType facilityType);

    List<HealthFacility> findByRegionRegionIdAndFacilityType(Long regionId, FacilityType facilityType);
}
