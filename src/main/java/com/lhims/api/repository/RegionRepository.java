package com.lhims.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lhims.api.domain.entity.Region;

public interface RegionRepository extends JpaRepository<Region, Long> {
    List<Region> findByNameContainingIgnoreCase(String name);
}
