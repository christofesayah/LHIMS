package com.lhims.api.repository;

import com.lhims.api.domain.entity.FacilityCapacity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacilityCapacityRepository extends JpaRepository<FacilityCapacity, Long> {
    Optional<FacilityCapacity> findTopByFacilityFacilityIdOrderByReportingDateDesc(Long facilityId);

    List<FacilityCapacity> findByFacilityFacilityIdOrderByReportingDateDesc(Long facilityId);
}
