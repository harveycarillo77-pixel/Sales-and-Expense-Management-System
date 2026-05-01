package com.example.sales_expense_system.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import com.example.sales_expense_system.dto.request.SaleRequest;
import com.example.sales_expense_system.dto.request.SaleUpdateRequest;
import com.example.sales_expense_system.model.Sale;

import jakarta.servlet.http.HttpServletRequest;

public interface SaleService {

    List<Sale> getAllSales();

    List<Sale> getAllSalesIncludingVoided();

    Sale getSaleById(Long id);

    Sale createSale(SaleRequest request, HttpServletRequest requestHttp);

    Sale updateSale(Long id, SaleUpdateRequest request, HttpServletRequest requestHttp);

    // version added — required for optimistic lock check on void
    Sale voidSale(Long id, String reason, Long version, HttpServletRequest requestHttp);

    void permanentDeleteSale(Long id, HttpServletRequest requestHttp);

    List<Sale> searchSales(String keyword);

    BigDecimal getDailySalesTotal(LocalDate date);

    BigDecimal getMonthlySalesTotal(int month, int year);

    Page<Sale> getSalesAdvanced(
        String keyword,
        LocalDate startDate,
        LocalDate endDate,
        Long categoryId,
        boolean includeVoided,
        int page,
        int size,
        Sort sort
    );
}