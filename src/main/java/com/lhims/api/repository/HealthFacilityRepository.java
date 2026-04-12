package com.lhims.api.repository;

import com.lhims.api.domain.entity.HealthFacility;
import com.lhims.api.domain.enums.FacilityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HealthFacilityRepository extends JpaRepository<HealthFacility, Long> {
    List<HealthFacility> findByRegionRegionId(Long regionId);

    List<HealthFacility> findByFacilityType(FacilityType facilityType);

    List<HealthFacility> findByRegionRegionIdAndFacilityType(Long regionId, FacilityType facilityType);
}
