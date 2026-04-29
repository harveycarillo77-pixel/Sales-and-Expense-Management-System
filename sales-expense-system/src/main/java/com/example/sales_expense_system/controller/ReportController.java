package com.example.sales_expense_system.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.sales_expense_system.dto.ReportDto;
import com.example.sales_expense_system.service.ReportService;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<ReportDto> getSummary() {
        return ResponseEntity.ok(reportService.getSummary());
    }

    @GetMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<ReportDto> generateReport(
            @RequestParam(defaultValue = "monthly") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "false") boolean includeVoided
    ) {
        return ResponseEntity.ok(reportService.generateReport(period, startDate, endDate, includeVoided));
    }
}