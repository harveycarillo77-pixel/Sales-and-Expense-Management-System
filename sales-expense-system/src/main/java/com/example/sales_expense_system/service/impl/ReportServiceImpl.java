package com.example.sales_expense_system.service.impl;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.sales_expense_system.dto.ReportDto;
import com.example.sales_expense_system.dto.ReportDto.CategoryTotalDto;
import com.example.sales_expense_system.dto.ReportDto.TransactionRowDto;
import com.example.sales_expense_system.model.Expense;
import com.example.sales_expense_system.model.Sale;
import com.example.sales_expense_system.repository.ExpenseRepository;
import com.example.sales_expense_system.repository.SaleRepository;
import com.example.sales_expense_system.service.ReportService;

@Service
public class ReportServiceImpl implements ReportService {

    private final SaleRepository saleRepository;
    private final ExpenseRepository expenseRepository;

    public ReportServiceImpl(SaleRepository saleRepository, ExpenseRepository expenseRepository) {
        this.saleRepository = saleRepository;
        this.expenseRepository = expenseRepository;
    }

    @Override
    public ReportDto getSummary() {
        LocalDate today = LocalDate.now();
        return buildReport("summary", LocalDate.of(today.getYear(), 1, 1), today, false);
    }

    @Override
    public ReportDto generateReport(String period, LocalDate startDate, LocalDate endDate, boolean includeVoided) {
        LocalDate today = LocalDate.now();
        String normalized = period == null ? "monthly" : period.trim().toLowerCase();

        switch (normalized) {
            case "daily":
                startDate = today;
                endDate = today;
                break;
            case "weekly":
                startDate = today.with(DayOfWeek.MONDAY);
                endDate = today;
                break;
            case "monthly":
                startDate = today.withDayOfMonth(1);
                endDate = today;
                break;
            case "custom":
                if (startDate == null || endDate == null) {
                    throw new IllegalArgumentException("Start date and end date are required for custom reports.");
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid report period.");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be later than end date.");
        }

        return buildReport(normalized, startDate, endDate, includeVoided);
    }

    private ReportDto buildReport(String period, LocalDate startDate, LocalDate endDate, boolean includeVoided) {
        List<Sale> allSalesInRange = saleRepository.findAll().stream()
                .filter(sale -> sale.getTransactionDate() != null)
                .filter(sale -> !sale.getTransactionDate().isBefore(startDate) && !sale.getTransactionDate().isAfter(endDate))
                .toList();

        List<Expense> allExpensesInRange = expenseRepository.findAll().stream()
                .filter(expense -> expense.getExpenseDate() != null)
                .filter(expense -> !expense.getExpenseDate().isBefore(startDate) && !expense.getExpenseDate().isAfter(endDate))
                .toList();

        List<Sale> completedSales = allSalesInRange.stream()
                .filter(sale -> !isSaleVoided(sale))
                .toList();

        List<Expense> completedExpenses = allExpensesInRange.stream()
                .filter(expense -> !isExpenseVoided(expense))
                .toList();

        List<Sale> displaySales = includeVoided ? allSalesInRange : completedSales;
        List<Expense> displayExpenses = includeVoided ? allExpensesInRange : completedExpenses;

        BigDecimal totalSales = completedSales.stream()
                .map(sale -> safe(sale.getTotalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = completedExpenses.stream()
                .map(expense -> safe(expense.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netProfit = totalSales.subtract(totalExpenses);

        ReportDto dto = new ReportDto();
        dto.setPeriod(period);
        dto.setStartDate(startDate.toString());
        dto.setEndDate(endDate.toString());
        dto.setIncludeVoided(includeVoided);

        dto.setTotalSales(totalSales);
        dto.setTotalExpenses(totalExpenses);
        dto.setNetProfit(netProfit);

        dto.setCompletedSalesCount(completedSales.size());
        dto.setCompletedExpenseCount(completedExpenses.size());
        dto.setCompletedTransactionCount(completedSales.size() + completedExpenses.size());

        dto.setVoidedSalesCount(allSalesInRange.size() - completedSales.size());
        dto.setVoidedExpenseCount(allExpensesInRange.size() - completedExpenses.size());
        dto.setVoidedTransactionCount(
                dto.getVoidedSalesCount() + dto.getVoidedExpenseCount()
        );

        dto.setSalesCount(displaySales.size());
        dto.setExpenseCount(displayExpenses.size());
        dto.setTransactionCount(dto.getCompletedTransactionCount());

        dto.setSalesByCategory(buildSalesByCategory(completedSales));
        dto.setExpensesByCategory(buildExpensesByCategory(completedExpenses));
        dto.setTransactions(buildTransactions(displaySales, displayExpenses));

        return dto;
    }

    private List<CategoryTotalDto> buildSalesByCategory(List<Sale> sales) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();

        for (Sale sale : sales) {
            String categoryName = sale.getCategory() != null && sale.getCategory().getCategoryName() != null
                    ? sale.getCategory().getCategoryName()
                    : "Uncategorized";

            totals.put(categoryName, totals.getOrDefault(categoryName, BigDecimal.ZERO).add(safe(sale.getTotalAmount())));
        }

        return totals.entrySet().stream()
                .map(entry -> new CategoryTotalDto(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                .toList();
    }

    private List<CategoryTotalDto> buildExpensesByCategory(List<Expense> expenses) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();

        for (Expense expense : expenses) {
            String categoryName = expense.getCategory() != null && expense.getCategory().getCategoryName() != null
                    ? expense.getCategory().getCategoryName()
                    : "Uncategorized";

            totals.put(categoryName, totals.getOrDefault(categoryName, BigDecimal.ZERO).add(safe(expense.getAmount())));
        }

        return totals.entrySet().stream()
                .map(entry -> new CategoryTotalDto(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                .toList();
    }

    private List<TransactionRowDto> buildTransactions(List<Sale> sales, List<Expense> expenses) {
        List<TransactionRowDto> rows = new ArrayList<>();

        for (Sale sale : sales) {
            rows.add(new TransactionRowDto(
                    "Sale",
                    getSaleReference(sale),
                    sale.getTransactionDate() != null ? sale.getTransactionDate().toString() : "",
                    firstNonBlank(sale.getItemName(), sale.getCustomerName(), "—"),
                    sale.getCategory() != null && sale.getCategory().getCategoryName() != null
                            ? sale.getCategory().getCategoryName()
                            : "Uncategorized",
                    isSaleVoided(sale) ? "Voided" : "Active",
                    safe(sale.getTotalAmount())
            ));
        }

        for (Expense expense : expenses) {
            rows.add(new TransactionRowDto(
                    "Expense",
                    firstNonBlank(expense.getExpenseNumber(), "—"),
                    expense.getExpenseDate() != null ? expense.getExpenseDate().toString() : "",
                    firstNonBlank(expense.getDescription(), expense.getPayeeName(), "—"),
                    expense.getCategory() != null && expense.getCategory().getCategoryName() != null
                            ? expense.getCategory().getCategoryName()
                            : "Uncategorized",
                    isExpenseVoided(expense) ? "Voided" : "Active",
                    safe(expense.getAmount())
            ));
        }

        rows.sort(Comparator.comparing(TransactionRowDto::getDate).reversed());
        return rows;
    }

    private boolean isSaleVoided(Sale sale) {
        return Boolean.TRUE.equals(sale.getIsVoided());
    }

    private boolean isExpenseVoided(Expense expense) {
        return Boolean.TRUE.equals(expense.getIsVoided());
    }

    private String getSaleReference(Sale sale) {
        return firstNonBlank(
                sale.getSaleNumber(),
                "—"
        );
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}