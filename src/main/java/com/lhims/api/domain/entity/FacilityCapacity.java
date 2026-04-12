package com.lhims.api.domain.entity;

import com.lhims.api.domain.enums.OperationalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "facility_capacity")
public class FacilityCapacity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "capacity_id")
    private Long capacityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private HealthFacility facility;

    @Column(name = "reporting_date", nullable = false)
    private LocalDate reportingDate;

    @Column(name = "total_beds")
    private Integer totalBeds;

    @Column(name = "icu_beds")
    private Integer icuBeds;

    @Column(name = "doctors_count")
    private Integer doctorsCount;

    @Column(name = "nurses_count")
    private Integer nursesCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "operational_status")
    private OperationalStatus operationalStatus;

    public Long getCapacityId() {
        return capacityId;
    }

    public void setCapacityId(Long capacityId) {
        this.capacityId = capacityId;
    }

    public HealthFacility getFacility() {
        return facility;
    }

    public void setFacility(HealthFacility facility) {
        this.facility = facility;
    }

    public LocalDate getReportingDate() {
        return reportingDate;
    }

    public void setReportingDate(LocalDate reportingDate) {
        this.reportingDate = reportingDate;
    }

    public Integer getTotalBeds() {
        return totalBeds;
    }

    public void setTotalBeds(Integer totalBeds) {
        this.totalBeds = totalBeds;
    }

    public Integer getIcuBeds() {
        return icuBeds;
    }

    public void setIcuBeds(Integer icuBeds) {
        this.icuBeds = icuBeds;
    }

    public Integer getDoctorsCount() {
        return doctorsCount;
    }

    public void setDoctorsCount(Integer doctorsCount) {
        this.doctorsCount = doctorsCount;
    }

    public Integer getNursesCount() {
        return nursesCount;
    }

    public void setNursesCount(Integer nursesCount) {
        this.nursesCount = nursesCount;
    }

    public OperationalStatus getOperationalStatus() {
        return operationalStatus;
    }

    public void setOperationalStatus(OperationalStatus operationalStatus) {
        this.operationalStatus = operationalStatus;
    }

    public FacilityCapacity() {
    }
}
