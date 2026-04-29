package com.example.sales_expense_system.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.example.sales_expense_system.dto.DashboardDto;
import com.example.sales_expense_system.repository.ExpenseRepository;
import com.example.sales_expense_system.repository.SaleRepository;
import com.example.sales_expense_system.service.DashboardService;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final SaleRepository saleRepository;
    private final ExpenseRepository expenseRepository;

    public DashboardServiceImpl(SaleRepository saleRepository,
                                ExpenseRepository expenseRepository) {
        this.saleRepository = saleRepository;
        this.expenseRepository = expenseRepository;
    }

    @Override
    public DashboardDto getDashboardSummary() {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int year = today.getYear();

        BigDecimal todaySales = saleRepository.sumSalesByDate(today);
        BigDecimal todayExpenses = expenseRepository.sumExpensesByDate(today);
        BigDecimal todayProfit = todaySales.subtract(todayExpenses);

        BigDecimal monthSales = saleRepository.sumSalesByMonthAndYear(month, year);
        BigDecimal monthExpenses = expenseRepository.sumExpensesByMonthAndYear(month, year);
        BigDecimal monthProfit = monthSales.subtract(monthExpenses);

        DashboardDto dto = new DashboardDto();
        dto.setTodaySales(todaySales);
        dto.setTodayExpenses(todayExpenses);
        dto.setTodayProfit(todayProfit);
        dto.setMonthSales(monthSales);
        dto.setMonthExpenses(monthExpenses);
        dto.setMonthProfit(monthProfit);

        return dto;
    }
}