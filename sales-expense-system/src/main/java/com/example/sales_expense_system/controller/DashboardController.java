package com.example.sales_expense_system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.sales_expense_system.dto.DashboardDto;
import com.example.sales_expense_system.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<DashboardDto> getDashboardSummary() {
        return ResponseEntity.ok(dashboardService.getDashboardSummary());
    }
}