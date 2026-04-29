package com.example.sales_expense_system.service;

import java.time.LocalDate;

import com.example.sales_expense_system.dto.ReportDto;

public interface ReportService {

    ReportDto getSummary();

    ReportDto generateReport(String period, LocalDate startDate, LocalDate endDate, boolean includeVoided);
}