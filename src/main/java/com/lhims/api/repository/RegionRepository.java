package com.lhims.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lhims.api.domain.entity.Region;
import com.lhims.api.domain.enums.RegionType;

public interface RegionRepository extends JpaRepository<Region, Long> {
    List<Region> findByNameContainingIgnoreCase(String name);
    List<Region> findAllByType(RegionType type);
    List<Region> findByNameContainingIgnoreCaseAndType(String name, RegionType type);
    Optional<Region> findByRegionIdAndType(Long regionId, RegionType type);
}
