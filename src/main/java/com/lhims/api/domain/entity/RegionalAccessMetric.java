package com.lhims.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "regional_access_metrics")
public class RegionalAccessMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "access_metric_id")
    private Long accessMetricId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(name = "reporting_period", nullable = false)
    private String reportingPeriod;

    @Column(name = "hospitals_per_capita")
    private Double hospitalsPerCapita;

    @Column(name = "doctors_per_capita")
    private Double doctorsPerCapita;

    @Column(name = "icu_beds_per_capita")
    private Double icuBedsPerCapita;

    public Long getAccessMetricId() {
        return accessMetricId;
    }

    public void setAccessMetricId(Long accessMetricId) {
        this.accessMetricId = accessMetricId;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public String getReportingPeriod() {
        return reportingPeriod;
    }

    public void setReportingPeriod(String reportingPeriod) {
        this.reportingPeriod = reportingPeriod;
    }

    public Double getHospitalsPerCapita() {
        return hospitalsPerCapita;
    }

    public void setHospitalsPerCapita(Double hospitalsPerCapita) {
        this.hospitalsPerCapita = hospitalsPerCapita;
    }

    public Double getDoctorsPerCapita() {
        return doctorsPerCapita;
    }

    public void setDoctorsPerCapita(Double doctorsPerCapita) {
        this.doctorsPerCapita = doctorsPerCapita;
    }

    public Double getIcuBedsPerCapita() {
        return icuBedsPerCapita;
    }

    public void setIcuBedsPerCapita(Double icuBedsPerCapita) {
        this.icuBedsPerCapita = icuBedsPerCapita;
    }

    protected RegionalAccessMetric() {
    }
}
