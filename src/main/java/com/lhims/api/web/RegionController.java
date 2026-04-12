package com.lhims.api.web;

import com.lhims.api.service.RegionService;
import com.lhims.api.web.dto.RegionDtos;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/districts")
public class RegionController {

    private final RegionService regionService;

    public RegionController(RegionService regionService) {
        this.regionService = regionService;
    }

    @GetMapping
    public List<RegionDtos.DistrictSummary> getAll() {
        return regionService.getAllDistricts();
    }

    @GetMapping("/{id}")
    public RegionDtos.DistrictDetail getOne(@PathVariable Long id) {
        return regionService.getDistrict(id);
    }

    @GetMapping("/geojson")
    public RegionDtos.DistrictGeoJsonResponse geoJson() {
        return regionService.getGeoJson();
    }

    @GetMapping("/search")
    public List<RegionDtos.DistrictSummary> search(@RequestParam("q") String q) {
        return regionService.searchDistricts(q);
    }

    @GetMapping("/compare")
    @PreAuthorize("hasAnyRole('MINISTRY_OFFICIAL','HOSPITAL_ADMIN')")
    public RegionDtos.DistrictCompare compare(@RequestParam("a") Long a, @RequestParam("b") Long b) {
        return regionService.compare(a, b);
    }
}
