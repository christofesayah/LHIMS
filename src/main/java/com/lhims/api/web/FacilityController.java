package com.lhims.api.web;

import com.lhims.api.security.SecuritySupport;
import com.lhims.api.service.FacilityService;
import com.lhims.api.web.dto.FacilityDtos;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/facilities")
public class FacilityController {

    private final FacilityService facilityService;
    private final SecuritySupport securitySupport;

    public FacilityController(FacilityService facilityService, SecuritySupport securitySupport) {
        this.facilityService = facilityService;
        this.securitySupport = securitySupport;
    }

    @GetMapping
    public List<FacilityDtos.FacilityResponse> list(@RequestParam(value = "regionId", required = false) Long regionId) {
        if (regionId == null) {
            return facilityService.getAllFacilities();
        }
        return facilityService.getFacilitiesByRegion(regionId);
    }

    @GetMapping("/{id}")
    public FacilityDtos.FacilityResponse getOne(@PathVariable Long id) {
        return facilityService.getFacility(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('MINISTRY_OFFICIAL')")
    public FacilityDtos.FacilityResponse create(@Valid @RequestBody FacilityDtos.CreateFacilityRequest request,
                                                HttpServletRequest httpServletRequest) {
        return facilityService.addFacility(request, securitySupport.currentUser().userId(), httpServletRequest);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MINISTRY_OFFICIAL')")
    public FacilityDtos.FacilityResponse update(@PathVariable Long id,
                                                @Valid @RequestBody FacilityDtos.UpdateFacilityRequest request,
                                                HttpServletRequest httpServletRequest) {
        return facilityService.updateFacility(id, request, securitySupport.currentUser().userId(), httpServletRequest);
    }

    @PutMapping("/{id}/capacity")
    @PreAuthorize("hasAnyRole('MINISTRY_OFFICIAL','HOSPITAL_ADMIN')")
    public FacilityDtos.FacilityResponse updateCapacity(@PathVariable Long id,
                                                        @Valid @RequestBody FacilityDtos.UpdateCapacityRequest request,
                                                        HttpServletRequest httpServletRequest) {
        return facilityService.updateCapacity(id, request, securitySupport.currentUser().userId(), httpServletRequest);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MINISTRY_OFFICIAL')")
    public void delete(@PathVariable Long id, HttpServletRequest httpServletRequest) {
        facilityService.deleteFacility(id, securitySupport.currentUser().userId(), httpServletRequest);
    }
}
