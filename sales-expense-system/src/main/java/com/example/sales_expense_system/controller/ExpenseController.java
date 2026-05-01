package com.example.sales_expense_system.controller;

import com.example.sales_expense_system.dto.request.ExpenseRequest;
import com.example.sales_expense_system.dto.request.ExpenseUpdateRequest;
import com.example.sales_expense_system.dto.response.ExpenseResponse;
import com.example.sales_expense_system.dto.response.PaginatedResponse;
import com.example.sales_expense_system.model.Expense;
import com.example.sales_expense_system.service.ExpenseService;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<List<ExpenseResponse>> getAllExpenses(
            @RequestParam(required = false, defaultValue = "false") boolean includeVoided) {
        List<Expense> expenses = includeVoided
                ? expenseService.getAllExpensesIncludingVoided()
                : expenseService.getAllExpenses();
        List<ExpenseResponse> response = expenses.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<ExpenseResponse> createExpense(@Valid @RequestBody ExpenseRequest request, HttpServletRequest httpRequest) {
        Expense expense = expenseService.createExpense(request, httpRequest);
        return ResponseEntity.ok(mapToDto(expense));
    }

    @GetMapping("/advanced")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<PaginatedResponse<ExpenseResponse>> getExpensesAdvanced(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "false") boolean includeVoided,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "expenseDate,desc") String sort
    ) {
        Sort sortObj = parseSort(sort);

        Page<Expense> result = expenseService.getExpensesAdvanced(
                keyword,
                startDate != null ? LocalDate.parse(startDate) : null,
                endDate != null ? LocalDate.parse(endDate) : null,
                categoryId,
                includeVoided,
                page,
                size,
                sortObj
        );

        List<ExpenseResponse> content = result.getContent()
                .stream()
                .map(this::mapToDto)
                .toList();

        PaginatedResponse<ExpenseResponse> response =
                new PaginatedResponse<>(
                        content,
                        result.getNumber(),
                        result.getSize(),
                        result.getTotalElements(),
                        result.getTotalPages()
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<List<ExpenseResponse>> searchExpenses(@RequestParam String keyword) {
        List<ExpenseResponse> response = expenseService.searchExpenses(keyword)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<ExpenseResponse> getExpense(@PathVariable Long id) {
        return ResponseEntity.ok(mapToDto(expenseService.getExpenseById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<ExpenseResponse> updateExpense(@PathVariable Long id,
                                                         @Valid @RequestBody ExpenseUpdateRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(mapToDto(expenseService.updateExpense(id, request, httpRequest)));
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<ExpenseResponse> voidExpense(@PathVariable Long id,
                                                       @RequestParam String reason,
                                                       @RequestParam Long version,
                                                       HttpServletRequest httpRequest) {
        return ResponseEntity.ok(mapToDto(expenseService.voidExpense(id, reason, version, httpRequest)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<String> permanentDeleteExpense(@PathVariable Long id,
                                                     HttpServletRequest httpRequest) {
        expenseService.permanentDeleteExpense(id, httpRequest);
        return ResponseEntity.ok("Expense permanently deleted successfully");
    }

    @GetMapping("/reports/daily-total")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<BigDecimal> getDailyExpenseTotal(@RequestParam String date) {
        return ResponseEntity.ok(expenseService.getDailyExpenseTotal(LocalDate.parse(date)));
    }

    @GetMapping("/reports/monthly-total")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<BigDecimal> getMonthlyExpenseTotal(@RequestParam int month,
                                                             @RequestParam int year) {
        return ResponseEntity.ok(expenseService.getMonthlyExpenseTotal(month, year));
    }

    private ExpenseResponse mapToDto(Expense expense) {
        ExpenseResponse dto = new ExpenseResponse();
        dto.setExpenseId(expense.getExpenseId());
        dto.setExpenseNumber(expense.getExpenseNumber());
        dto.setDescription(expense.getDescription());
        dto.setPayeeName(expense.getPayeeName());
        dto.setCategoryId(expense.getCategory() != null ? expense.getCategory().getCategoryId() : null);
        dto.setCategoryName(expense.getCategory() != null ? expense.getCategory().getCategoryName() : null);
        dto.setAmount(expense.getAmount());
        dto.setExpenseDate(expense.getExpenseDate());
        dto.setVoided(Boolean.TRUE.equals(expense.getIsVoided()));
        dto.setVoidReason(expense.getVoidReason());
        dto.setVoidedAt(expense.getVoidedAt());
        dto.setVoidedByUserId(expense.getVoidedBy() != null ? expense.getVoidedBy().getUserId() : null);
        dto.setVoidedByUsername(expense.getVoidedBy() != null ? expense.getVoidedBy().getUsername() : null);
        dto.setCreatedByUserId(expense.getCreatedBy() != null ? expense.getCreatedBy().getUserId() : null);
        dto.setCreatedByUsername(expense.getCreatedBy() != null ? expense.getCreatedBy().getUsername() : null);
        dto.setVersion(expense.getVersion());
        return dto;
    }

    private Sort parseSort(String sort) {
        try {
            String[] parts = sort.split(",");
            String field = parts[0];

            // whitelist fields (VERY IMPORTANT for security)
            List<String> allowed = List.of(
                    "expenseDate",
                    "expenseId",
                    "expenseNumber",
                    "amount",
                    "description",
                    "payeeName"
            );

            if (!allowed.contains(field)) {
                field = "expenseDate";
            }

            Sort.Direction direction =
                    parts.length > 1 && parts[1].equalsIgnoreCase("asc")
                            ? Sort.Direction.ASC
                            : Sort.Direction.DESC;

            return Sort.by(direction, field);

        } catch (Exception e) {
            return Sort.by(Sort.Direction.DESC, "expenseDate");
        }
    }
}