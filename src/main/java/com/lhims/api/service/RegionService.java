package com.lhims.api.service;

import java.util.List;

import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lhims.api.domain.entity.ComputedScore;
import com.lhims.api.domain.entity.Region;
import com.lhims.api.domain.enums.RegionType;
import com.lhims.api.exception.NotFoundException;
import com.lhims.api.repository.ComputedScoreRepository;
import com.lhims.api.repository.RegionRepository;
import com.lhims.api.web.dto.RegionDtos;

@Service
public class RegionService {

    private final RegionRepository regionRepository;
    private final ComputedScoreRepository computedScoreRepository;

    public RegionService(RegionRepository regionRepository, ComputedScoreRepository computedScoreRepository) {
        this.regionRepository = regionRepository;
        this.computedScoreRepository = computedScoreRepository;
    }

    @Transactional(readOnly = true)
    public List<RegionDtos.DistrictSummary> getAllDistricts() {
        return regionRepository.findAllByType(RegionType.CAZA).stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public RegionDtos.DistrictDetail getDistrict(Long id) {
        Region region = regionRepository.findByRegionIdAndType(id, RegionType.CAZA)
                .orElseThrow(() -> new NotFoundException("Region not found"));
        return toDetail(region);
    }

    @Transactional(readOnly = true)
    public RegionDtos.DistrictGeoJsonResponse getGeoJson() {
        GeoJsonWriter writer = new GeoJsonWriter();
        List<RegionDtos.DistrictGeoJsonFeature> features = regionRepository.findAllByType(RegionType.CAZA).stream()
                .filter(region -> region.getGeometry() != null)
                .map(region -> new RegionDtos.DistrictGeoJsonFeature(
                        region.getRegionId(),
                        region.getName(),
                        writer.write(region.getGeometry())
                ))
                .toList();
        return new RegionDtos.DistrictGeoJsonResponse(features);
    }

    @Transactional(readOnly = true)
    public List<RegionDtos.DistrictSummary> searchDistricts(String query) {
        return regionRepository.findByNameContainingIgnoreCaseAndType(query, RegionType.CAZA).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public RegionDtos.DistrictCompare compare(Long a, Long b) {
        return new RegionDtos.DistrictCompare(getDistrict(a), getDistrict(b));
    }

    private RegionDtos.DistrictSummary toSummary(Region region) {
        ComputedScore score = computedScoreRepository.findTopByRegionRegionIdAndScenarioIsNullOrderByLastComputedAtDesc(region.getRegionId())
                .orElse(null);
        return new RegionDtos.DistrictSummary(
                region.getRegionId(),
                region.getName(),
                score == null ? null : score.getRiskCategory(),
                score == null ? null : score.getCiriScore()
        );
    }

    private RegionDtos.DistrictDetail toDetail(Region region) {
        ComputedScore score = computedScoreRepository.findTopByRegionRegionIdAndScenarioIsNullOrderByLastComputedAtDesc(region.getRegionId())
                .orElse(null);
        return new RegionDtos.DistrictDetail(
                region.getRegionId(),
                region.getName(),
                region.getType(),
                region.getPopulation(),
                score == null ? null : score.getRiskCategory(),
                score == null ? null : score.getHaiScore(),
                score == null ? null : score.getRviScore(),
                score == null ? null : score.getCiriScore()
        );
    }
}
