package com.lhims.api.web.dto;

import com.lhims.api.domain.enums.FacilityType;
import com.lhims.api.domain.enums.OperationalStatus;
import com.lhims.api.domain.enums.OwnershipType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public final class FacilityDtos {

    private FacilityDtos() {
    }

    public record CreateFacilityRequest(@NotNull Long regionId, @NotBlank String name, @NotNull FacilityType facilityType,
                                        @NotNull OwnershipType ownershipType, Double latitude, Double longitude,
                                        Integer totalBeds, Integer icuBeds, Integer doctorsCount, Integer nursesCount,
                                        OperationalStatus operationalStatus) {
    }

    public record UpdateFacilityRequest(String name, FacilityType facilityType, OwnershipType ownershipType,
                                        Double latitude, Double longitude) {
    }

    public record UpdateCapacityRequest(Integer totalBeds, Integer icuBeds, Integer doctorsCount, Integer nursesCount,
                                        OperationalStatus operationalStatus) {
    }

    public record FacilityResponse(Long facilityId, Long regionId, String name, FacilityType facilityType,
                                   OwnershipType ownershipType, Double latitude, Double longitude,
                                   CapacityResponse latestCapacity) {
    }

    public record CapacityResponse(LocalDate reportingDate, Integer totalBeds, Integer icuBeds,
                                   Integer doctorsCount, Integer nursesCount, OperationalStatus operationalStatus,
                                   String updatedBy) {
    }
}
