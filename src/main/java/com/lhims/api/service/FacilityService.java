package com.lhims.api.service;

import com.lhims.api.domain.entity.FacilityCapacity;
import com.lhims.api.domain.entity.HealthFacility;
import com.lhims.api.domain.enums.AuditActionType;
import com.lhims.api.exception.NotFoundException;
import com.lhims.api.repository.FacilityCapacityRepository;
import com.lhims.api.repository.HealthFacilityRepository;
import com.lhims.api.repository.RegionRepository;
import com.lhims.api.web.dto.FacilityDtos;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class FacilityService {

    private final HealthFacilityRepository healthFacilityRepository;
    private final FacilityCapacityRepository facilityCapacityRepository;
    private final RegionRepository regionRepository;
    private final AuditService auditService;

    public FacilityService(HealthFacilityRepository healthFacilityRepository,
                           FacilityCapacityRepository facilityCapacityRepository,
                           RegionRepository regionRepository,
                           AuditService auditService) {
        this.healthFacilityRepository = healthFacilityRepository;
        this.facilityCapacityRepository = facilityCapacityRepository;
        this.regionRepository = regionRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<FacilityDtos.FacilityResponse> getFacilitiesByRegion(Long regionId) {
        return healthFacilityRepository.findByRegionRegionId(regionId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public FacilityDtos.FacilityResponse getFacility(Long facilityId) {
        HealthFacility facility = healthFacilityRepository.findById(facilityId)
                .orElseThrow(() -> new NotFoundException("Facility not found"));
        return toDto(facility);
    }

    @Transactional
    public FacilityDtos.FacilityResponse addFacility(FacilityDtos.CreateFacilityRequest request, Long actorUserId,
                                                     HttpServletRequest httpServletRequest) {
        HealthFacility facility = new HealthFacility();
        facility.setRegion(regionRepository.findById(request.regionId())
                .orElseThrow(() -> new NotFoundException("Region not found")));
        facility.setName(request.name());
        facility.setFacilityType(request.facilityType());
        facility.setOwnershipType(request.ownershipType());
        facility.setLatitude(request.latitude());
        facility.setLongitude(request.longitude());
        HealthFacility saved = healthFacilityRepository.save(facility);

        createCapacity(saved, request.totalBeds(), request.icuBeds(), request.doctorsCount(), request.nursesCount(),
                request.operationalStatus());

        auditService.log(actorUserId, null, AuditActionType.CREATE, "health_facilities",
                String.valueOf(saved.getFacilityId()), null, "ADD_FACILITY", httpServletRequest);
        return toDto(saved);
    }

    @Transactional
    public FacilityDtos.FacilityResponse updateFacility(Long facilityId, FacilityDtos.UpdateFacilityRequest request,
                                                        Long actorUserId, HttpServletRequest httpServletRequest) {
        HealthFacility facility = healthFacilityRepository.findById(facilityId)
                .orElseThrow(() -> new NotFoundException("Facility not found"));
        if (request.name() != null) facility.setName(request.name());
        if (request.facilityType() != null) facility.setFacilityType(request.facilityType());
        if (request.ownershipType() != null) facility.setOwnershipType(request.ownershipType());
        if (request.latitude() != null) facility.setLatitude(request.latitude());
        if (request.longitude() != null) facility.setLongitude(request.longitude());
        HealthFacility saved = healthFacilityRepository.save(facility);
        auditService.log(actorUserId, null, saved.getRegion().getRegionId(), AuditActionType.UPDATE, "health_facilities",
                String.valueOf(saved.getFacilityId()), null, "UPDATE_FACILITY", httpServletRequest);
        return toDto(saved);
    }

    @Transactional
    public FacilityDtos.FacilityResponse updateCapacity(Long facilityId, FacilityDtos.UpdateCapacityRequest request,
                                                        Long actorUserId, HttpServletRequest httpServletRequest) {
        HealthFacility facility = healthFacilityRepository.findById(facilityId)
                .orElseThrow(() -> new NotFoundException("Facility not found"));
        createCapacity(facility, request.totalBeds(), request.icuBeds(), request.doctorsCount(), request.nursesCount(),
                request.operationalStatus());
        auditService.log(actorUserId, null, facility.getRegion().getRegionId(), AuditActionType.UPDATE, "facility_capacity",
                String.valueOf(facilityId), null, "UPDATE_CAPACITY", httpServletRequest);
        return toDto(facility);
    }

    @Transactional
    public void deleteFacility(Long facilityId, Long actorUserId, HttpServletRequest httpServletRequest) {
        HealthFacility facility = healthFacilityRepository.findById(facilityId)
                .orElseThrow(() -> new NotFoundException("Facility not found"));
        Long regionId = facility.getRegion() == null ? null : facility.getRegion().getRegionId();
        healthFacilityRepository.delete(facility);
        auditService.log(actorUserId, null, regionId, AuditActionType.DELETE, "health_facilities",
                String.valueOf(facilityId), null, "DELETE_FACILITY", httpServletRequest);
    }

    private void createCapacity(HealthFacility facility, Integer totalBeds, Integer icuBeds, Integer doctorsCount,
                                Integer nursesCount, com.lhims.api.domain.enums.OperationalStatus operationalStatus) {
        FacilityCapacity capacity = new FacilityCapacity();
        capacity.setFacility(facility);
        capacity.setReportingDate(LocalDate.now());
        capacity.setTotalBeds(totalBeds);
        capacity.setIcuBeds(icuBeds);
        capacity.setDoctorsCount(doctorsCount);
        capacity.setNursesCount(nursesCount);
        capacity.setOperationalStatus(operationalStatus);
        facilityCapacityRepository.save(capacity);
    }

    private FacilityDtos.FacilityResponse toDto(HealthFacility facility) {
        FacilityCapacity latest = facilityCapacityRepository.findTopByFacilityFacilityIdOrderByReportingDateDesc(facility.getFacilityId())
                .orElse(null);
        FacilityDtos.CapacityResponse capacity = latest == null ? null : new FacilityDtos.CapacityResponse(
                latest.getReportingDate(), latest.getTotalBeds(), latest.getIcuBeds(), latest.getDoctorsCount(),
                latest.getNursesCount(), latest.getOperationalStatus()
        );
        return new FacilityDtos.FacilityResponse(
                facility.getFacilityId(),
                facility.getRegion().getRegionId(),
                facility.getName(),
                facility.getFacilityType(),
                facility.getOwnershipType(),
                facility.getLatitude(),
                facility.getLongitude(),
                capacity
        );
    }
}
