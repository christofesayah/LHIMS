package com.lhims.api.web;

import com.lhims.api.service.ExportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/districts/{id}/pdf")
    @PreAuthorize("hasRole('MINISTRY_OFFICIAL')")
    public ResponseEntity<byte[]> districtPdf(@PathVariable Long id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("district-" + id + ".pdf").build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(exportService.districtPdf(id));
    }

    @GetMapping("/districts/csv")
    @PreAuthorize("hasRole('MINISTRY_OFFICIAL')")
    public ResponseEntity<byte[]> districtsCsv() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("districts.csv").build().toString())
                .contentType(MediaType.valueOf("text/csv"))
                .body(exportService.districtsCsv());
    }

    @GetMapping("/scenarios/{id}/pdf")
    @PreAuthorize("hasAnyRole('MINISTRY_OFFICIAL','HOSPITAL_ADMIN')")
    public ResponseEntity<byte[]> scenarioPdf(@PathVariable Long id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("scenario-" + id + ".pdf").build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(exportService.scenarioPdf(id));
    }
}
