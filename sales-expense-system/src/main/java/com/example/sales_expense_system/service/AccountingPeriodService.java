package com.example.sales_expense_system.service;

import java.util.List;

import com.example.sales_expense_system.dto.request.AccountingPeriodRequest;
import com.example.sales_expense_system.model.AccountingPeriod;

import jakarta.servlet.http.HttpServletRequest;

public interface AccountingPeriodService {

    List<AccountingPeriod> getAllPeriods();

    AccountingPeriod getPeriodById(Long id);

    AccountingPeriod createPeriod(AccountingPeriodRequest request, HttpServletRequest httpRequest);

    AccountingPeriod updatePeriod(Long id, AccountingPeriodRequest request, HttpServletRequest httpRequest);

    AccountingPeriod lockPeriod(Long id, HttpServletRequest httpRequest);

    AccountingPeriod unlockPeriod(Long id, HttpServletRequest httpRequest);

    void deletePeriod(Long id, HttpServletRequest httpRequest);

    void validateDateNotInLockedPeriod(java.time.LocalDate transactionDate);

}
