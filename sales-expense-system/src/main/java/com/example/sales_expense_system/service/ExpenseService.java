package com.example.sales_expense_system.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import com.example.sales_expense_system.dto.request.ExpenseRequest;
import com.example.sales_expense_system.dto.request.ExpenseUpdateRequest;
import com.example.sales_expense_system.model.Expense;

import jakarta.servlet.http.HttpServletRequest;

public interface ExpenseService {

    List<Expense> getAllExpenses();

    List<Expense> getAllExpensesIncludingVoided();

    Expense getExpenseById(Long id);

    Expense createExpense(ExpenseRequest request, HttpServletRequest httpRequest);

    Expense updateExpense(Long id, ExpenseUpdateRequest request, HttpServletRequest httpRequest);

    Expense voidExpense(Long id, String reason, Long version, HttpServletRequest httpRequest);

    List<Expense> searchExpenses(String keyword);

    BigDecimal getDailyExpenseTotal(LocalDate date);

    BigDecimal getMonthlyExpenseTotal(int month, int year);

    void permanentDeleteExpense(Long id, HttpServletRequest httpRequest);

    Page<Expense> getExpensesAdvanced(
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