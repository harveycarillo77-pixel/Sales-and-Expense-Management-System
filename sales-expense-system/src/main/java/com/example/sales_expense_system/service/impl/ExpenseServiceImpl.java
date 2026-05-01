package com.example.sales_expense_system.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;

import jakarta.servlet.http.HttpServletRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.sales_expense_system.dto.AuditFieldChangeDto;
import com.example.sales_expense_system.dto.request.ExpenseRequest;
import com.example.sales_expense_system.dto.request.ExpenseUpdateRequest;
import com.example.sales_expense_system.exception.ApiException;
import com.example.sales_expense_system.model.AccountingPeriod;
import com.example.sales_expense_system.model.Category;
import com.example.sales_expense_system.model.Expense;
import com.example.sales_expense_system.model.User;
import com.example.sales_expense_system.repository.AccountingPeriodRepository;
import com.example.sales_expense_system.repository.CategoryRepository;
import com.example.sales_expense_system.repository.ExpenseRepository;
import com.example.sales_expense_system.repository.UserRepository;
import com.example.sales_expense_system.service.AuditLogService;
import com.example.sales_expense_system.service.ExpenseService;
import com.example.sales_expense_system.util.AuditActions;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ExpenseServiceImpl implements ExpenseService {

    private static final Logger log = LoggerFactory.getLogger(ExpenseServiceImpl.class);

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final AccountingPeriodRepository periodRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public ExpenseServiceImpl(ExpenseRepository expenseRepository,
                              UserRepository userRepository,
                              CategoryRepository categoryRepository,
                              AccountingPeriodRepository periodRepository,
                              AuditLogService auditLogService,
                              ObjectMapper objectMapper) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.periodRepository = periodRepository;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Expense> getAllExpenses() {
        List<Expense> expenses = expenseRepository.findByIsVoidedFalseOrderByExpenseDateDescExpenseIdDesc();
        expenses.forEach(this::initializeExpenseRelations);
        return expenses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Expense> getAllExpensesIncludingVoided() {
        List<Expense> expenses = expenseRepository.findAllByOrderByExpenseDateDescExpenseIdDesc();
        expenses.forEach(this::initializeExpenseRelations);
        return expenses;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Expense> getExpensesAdvanced(
            String keyword,
            LocalDate startDate,
            LocalDate endDate,
            Long categoryId,
            boolean includeVoided,
            int page,
            int size,
            Sort sort) {

        if (size > 100) size = 100; // prevent abuse

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Expense> result = expenseRepository.searchWithFilters(
            keyword == null || keyword.isBlank() ? null : keyword.trim(),
            categoryId,
            startDate,
            endDate,
            includeVoided,
            pageable
        );

        result.getContent().forEach(this::initializeExpenseRelations);

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Expense getExpenseById(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ApiException("Expense not found", HttpStatus.NOT_FOUND));
        initializeExpenseRelations(expense);
        return expense;
    }

    @Override
    @Transactional
    public Expense createExpense(ExpenseRequest request, HttpServletRequest httpRequest) {
        Category category = categoryRepository.findById(request.getCategoryId().longValue())
                .orElseThrow(() -> new ApiException("Category not found", HttpStatus.NOT_FOUND));

        if (category.getType() != Category.CategoryType.EXPENSE) {
            throw new ApiException("Selected category is not an EXPENSE category", HttpStatus.BAD_REQUEST);
        }

        AccountingPeriod period = findPeriodByExpenseDate(request.getExpenseDate());
        validatePeriodNotLocked(period);

        User currentUser = getCurrentUser();

        String expenseNumber = null;
        if (request.getExpenseNumber() != null && !request.getExpenseNumber().trim().isEmpty()) {
            expenseNumber = request.getExpenseNumber().trim();
        }

        Expense expense = new Expense();
        expense.setExpenseNumber(expenseNumber);
        expense.setExpenseDate(request.getExpenseDate());
        expense.setDescription(request.getDescription().trim());
        expense.setPayeeName(request.getPayeeName() != null ? request.getPayeeName().trim() : null);
        expense.setCategory(category);
        expense.setAmount(request.getAmount());
        expense.setIsVoided(false);
        expense.setCreatedBy(currentUser);
        expense.setPeriod(period);

        Expense savedExpense;

        try {
            savedExpense = expenseRepository.save(expense);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ApiException("Expense number already exists", HttpStatus.CONFLICT);
        }

        List<AuditFieldChangeDto> changes = buildCreateChanges(savedExpense);

        auditLogService.log(
                AuditActions.CREATE_EXPENSE,
                "Expenses",
                "expenses",
                savedExpense.getExpenseId(),
                expenseTargetDisplay(savedExpense),
                "Created expense " + expenseTargetDisplay(savedExpense) + ".",
                "New expense record was added.",
                null,
                expenseSnapshot(savedExpense),
                toJson(changes),
                currentUser,
                httpRequest
        );

        initializeExpenseRelations(savedExpense);
        return savedExpense;
    }

    @Override
    @Transactional
    public Expense updateExpense(Long id, ExpenseUpdateRequest request, HttpServletRequest httpRequest) {
        Expense existing = expenseRepository.findById(id)
                .orElseThrow(() -> new ApiException("Expense not found", HttpStatus.NOT_FOUND));
        initializeExpenseRelations(existing);

        // 🚨 STEP 2A — BLOCK if already VOIDED
        if (Boolean.TRUE.equals(existing.getIsVoided())) {
            throw new ApiException("Voided sales cannot be edited", HttpStatus.CONFLICT);
        }

        // 🚨 STEP 2B — OPTIMISTIC LOCK CHECK (DEBUG)
        log.warn(">>> VERSION CHECK: DB version={} (type={}), Request version={} (type={})",
            existing.getVersion(),
            existing.getVersion() == null ? "null" : existing.getVersion().getClass().getSimpleName(),
            request.getVersion(),
            request.getVersion() == null ? "null" : request.getVersion().getClass().getSimpleName()
        );
        if (request.getVersion() == null || !existing.getVersion().equals(request.getVersion())) {
            log.warn(">>> VERSION MISMATCH — throwing 409. DB={}, Request={}", existing.getVersion(), request.getVersion());
            throw new ApiException("Data has been modified by another user", HttpStatus.CONFLICT);
        }
        log.warn(">>> VERSION OK — proceeding with update");

        validatePeriodNotLocked(existing.getPeriod());

        User currentUser = getCurrentUser();
        validateCanModifyExpense(existing, currentUser);

        String oldExpenseNumber = existing.getExpenseNumber();
        String oldExpenseDate = String.valueOf(existing.getExpenseDate());
        String oldDescription = existing.getDescription();
        String oldPayeeName = existing.getPayeeName();
        String oldCategoryName = existing.getCategory() != null ? existing.getCategory().getCategoryName() : "";
        String oldAmount = stringifyNumber(existing.getAmount());
        String oldPeriodName = existing.getPeriod() != null ? existing.getPeriod().getName() : "";
        String oldValues = expenseSnapshot(existing);

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId().longValue())
                    .orElseThrow(() -> new ApiException("Category not found", HttpStatus.NOT_FOUND));

            if (category.getType() != Category.CategoryType.EXPENSE) {
                throw new ApiException("Selected category is not an EXPENSE category", HttpStatus.BAD_REQUEST);
            }

            existing.setCategory(category);
        }

        if (request.getDescription() != null) {
            String description = request.getDescription().trim();
            if (description.isEmpty()) {
                throw new ApiException("Description must not be blank", HttpStatus.BAD_REQUEST);
            }
            existing.setDescription(description);
        }

        if (request.getPayeeName() != null) {
            String payeeName = request.getPayeeName().trim();
            existing.setPayeeName(payeeName.isEmpty() ? null : payeeName);
        }

        if (request.getAmount() != null) {
            if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ApiException("Amount must be greater than 0", HttpStatus.BAD_REQUEST);
            }
            existing.setAmount(request.getAmount());
        }

        if (request.getExpenseNumber() != null) {
            String trimmedExpenseNumber = request.getExpenseNumber().trim();

            if (!trimmedExpenseNumber.isEmpty()
                    && !trimmedExpenseNumber.equals(existing.getExpenseNumber())) {

                boolean exists = expenseRepository.existsByExpenseNumberAndExpenseIdNot(
                        trimmedExpenseNumber, existing.getExpenseId()
                );

                if (exists) {
                    throw new ApiException("Expense number already exists", HttpStatus.CONFLICT);
                }
            }

            existing.setExpenseNumber(trimmedExpenseNumber.isEmpty() ? null : trimmedExpenseNumber);
        }

        if (request.getExpenseDate() != null) {
            AccountingPeriod newPeriod = findPeriodByExpenseDate(request.getExpenseDate());
            validatePeriodNotLocked(newPeriod);
            existing.setExpenseDate(request.getExpenseDate());
            existing.setPeriod(newPeriod);
        }

        List<AuditFieldChangeDto> changes = new ArrayList<>();
        addChangedField(changes, "expenseNumber", oldExpenseNumber, existing.getExpenseNumber());
        addChangedField(changes, "expenseDate", oldExpenseDate, String.valueOf(existing.getExpenseDate()));
        addChangedField(changes, "description", oldDescription, existing.getDescription());
        addChangedField(changes, "payeeName", oldPayeeName, existing.getPayeeName());
        addChangedField(changes, "category", oldCategoryName, existing.getCategory() != null ? existing.getCategory().getCategoryName() : "");
        addChangedField(changes, "amount", oldAmount, stringifyNumber(existing.getAmount()));
        addChangedField(changes, "period", oldPeriodName, existing.getPeriod() != null ? existing.getPeriod().getName() : "");

        Expense updatedExpense;

        try {
            updatedExpense = expenseRepository.save(existing);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ApiException("Expense number already exists", HttpStatus.CONFLICT);
        }

        auditLogService.log(
                AuditActions.UPDATE_EXPENSE,
                "Expenses",
                "expenses",
                updatedExpense.getExpenseId(),
                expenseTargetDisplay(updatedExpense),
                "Updated expense " + expenseTargetDisplay(updatedExpense) + ".",
                changes.isEmpty()
                        ? "Expense record was saved with no detected field changes."
                        : "Expense details were updated.",
                oldValues,
                expenseSnapshot(updatedExpense),
                toJson(changes),
                currentUser,
                httpRequest
        );

        initializeExpenseRelations(updatedExpense);
        return updatedExpense;
    }

    @Override
    @Transactional
    public Expense voidExpense(Long id, String reason, Long version, HttpServletRequest httpRequest) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ApiException("Expense not found", HttpStatus.NOT_FOUND));
        initializeExpenseRelations(expense);

        validatePeriodNotLocked(expense.getPeriod());

        if (Boolean.TRUE.equals(expense.getIsVoided())) {
            throw new ApiException("Expense is already voided", HttpStatus.CONFLICT);
        }

        // Optimistic lock check: reject if the client's version doesn't match the DB.
        // This catches the case where another user edited the expense after this user
        // loaded it — expense.getVersion() is never null for a persisted entity, so
        // the old "== null" guard was a no-op that protected nothing.
        if (version == null || expense.getVersion() == null || !expense.getVersion().equals(version)) {
            throw new ApiException("Data has been modified by another user", HttpStatus.CONFLICT);
        }

        User currentUser = getCurrentUser();
        validateCanModifyExpense(expense, currentUser);

        String oldValues = expenseSnapshot(expense);

        expense.setIsVoided(true);
        expense.setVoidedBy(currentUser);
        expense.setVoidedAt(LocalDateTime.now());
        expense.setVoidReason(reason == null || reason.trim().isEmpty() ? null : reason.trim());

        Expense voidedExpense = expenseRepository.save(expense);

        List<AuditFieldChangeDto> changes = List.of(
            new AuditFieldChangeDto("isVoided", "false", "true"),
            new AuditFieldChangeDto("voidReason", "", safe(voidedExpense.getVoidReason())),
            new AuditFieldChangeDto("voidedBy", "", currentUser.getUsername()),
            new AuditFieldChangeDto("voidedAt", "", String.valueOf(voidedExpense.getVoidedAt()))
        );

        auditLogService.log(
                AuditActions.VOID_EXPENSE,
                "Expenses",
                "expenses",
                voidedExpense.getExpenseId(),
                expenseTargetDisplay(voidedExpense),
                "Voided expense " + expenseTargetDisplay(voidedExpense) + ".",
                voidedExpense.getVoidReason() == null
                        ? "Expense was marked as void."
                        : "Expense was marked as void. Reason: " + voidedExpense.getVoidReason(),
                oldValues,
                expenseSnapshot(voidedExpense),
                toJson(changes),
                currentUser,
                httpRequest
        );

        initializeExpenseRelations(voidedExpense);
        return voidedExpense;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Expense> searchExpenses(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return expenseRepository.findByIsVoidedFalseOrderByExpenseDateDescExpenseIdDesc();
        }

        String search = keyword.trim();

        List<Expense> byDescription =
                expenseRepository.findByDescriptionContainingIgnoreCaseAndIsVoidedFalseOrderByExpenseDateDescExpenseIdDesc(search);

        List<Expense> byExpenseNumber =
                expenseRepository.findByExpenseNumberContainingIgnoreCaseAndIsVoidedFalseOrderByExpenseDateDescExpenseIdDesc(search);

        List<Expense> results = new ArrayList<>(byDescription);

        for (Expense expense : byExpenseNumber) {
            if (!results.contains(expense)) {
                results.add(expense);
            }
        }

        results.forEach(this::initializeExpenseRelations);
        return results;
    }

    @Override
    public BigDecimal getDailyExpenseTotal(LocalDate date) {
        return expenseRepository.sumExpensesByDate(date);
    }

    @Override
    public BigDecimal getMonthlyExpenseTotal(int month, int year) {
        return expenseRepository.sumExpensesByMonthAndYear(month, year);
    }

    private AccountingPeriod findPeriodByExpenseDate(LocalDate expenseDate) {
        return periodRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(expenseDate, expenseDate)
                .orElseThrow(() -> new ApiException(
                "No accounting period found for expense date",
                HttpStatus.NOT_FOUND
            ));
    }

    private void validatePeriodNotLocked(AccountingPeriod period) {
        if (Boolean.TRUE.equals(period.getIsLocked())) {
            throw new ApiException(
                    "Accounting period is locked: " + period.getName()
                            + " (" + period.getStartDate() + " to " + period.getEndDate() + ")", HttpStatus.CONFLICT
            );
        }
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(
                "Authenticated user not found",
                HttpStatus.UNAUTHORIZED
            ));
    }

    private void validateCanModifyExpense(Expense expense, User currentUser) {
        boolean isAdmin = currentUser.getRole() != null
                && currentUser.getRole().getRoleName() != null
                && currentUser.getRole().getRoleName().equalsIgnoreCase("ADMIN");

        boolean isOwner = expense.getCreatedBy() != null
                && expense.getCreatedBy().getUserId().equals(currentUser.getUserId());

        if (!isAdmin && !isOwner) {
            throw new ApiException("You do not have permission to modify this expense", HttpStatus.FORBIDDEN);
        }
    }

    @Transactional
    @Override
    public void permanentDeleteExpense(Long id, HttpServletRequest httpRequest) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ApiException("Expense not found", HttpStatus.NOT_FOUND));

        if (!Boolean.TRUE.equals(expense.getIsVoided())) {
            throw new ApiException(
            "Only voided expenses can be permanently deleted",
            HttpStatus.BAD_REQUEST
        );
        }

        User admin = getCurrentUser();
        String oldValues = expenseSnapshot(expense);
        String targetDisplay = expenseTargetDisplay(expense);

        List<AuditFieldChangeDto> changes = List.of(
            new AuditFieldChangeDto("deleted", "false", "true")
        );

        auditLogService.log(
                AuditActions.PERMANENT_DELETE_EXPENSE,
                "Expenses",
                "expenses",
                expense.getExpenseId(),
                targetDisplay,
                "Permanently deleted expense " + targetDisplay + ".",
                "Voided expense record was permanently deleted.",
                oldValues,
                null,
                toJson(changes),
                admin,
                httpRequest
        );

        expenseRepository.delete(expense);
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String stringifyNumber(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    private String expenseTargetDisplay(Expense expense) {
        if (expense.getExpenseNumber() != null && !expense.getExpenseNumber().isBlank()) {
            return expense.getExpenseNumber();
        }
        return "Expense #" + expense.getExpenseId();
    }

    private String expenseSnapshot(Expense expense) {
        return """
            {
              "expenseNumber":"%s",
              "expenseDate":"%s",
              "description":"%s",
              "payeeName":"%s",
              "category":"%s",
              "amount":"%s",
              "isVoided":%s,
              "voidReason":"%s",
              "period":"%s"
            }
            """.formatted(
                escapeJson(expense.getExpenseNumber()),
                String.valueOf(expense.getExpenseDate()),
                escapeJson(expense.getDescription()),
                escapeJson(expense.getPayeeName()),
                escapeJson(expense.getCategory() != null ? expense.getCategory().getCategoryName() : ""),
                stringifyNumber(expense.getAmount()),
                Boolean.TRUE.equals(expense.getIsVoided()),
                escapeJson(expense.getVoidReason()),
                escapeJson(expense.getPeriod() != null ? expense.getPeriod().getName() : "")
            ).replace("\n", "").replace("  ", "");
    }

    private void addChangedField(List<AuditFieldChangeDto> changes,
                                 String field,
                                 Object oldValue,
                                 Object newValue) {

        String oldText = oldValue == null ? "" : String.valueOf(oldValue);
        String newText = newValue == null ? "" : String.valueOf(newValue);

        if (!oldText.equals(newText)) {
            changes.add(new AuditFieldChangeDto(field, oldText, newText));
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    private void initializeExpenseRelations(Expense expense) {
        if (expense == null) return;

        if (expense.getCreatedBy() != null) {
            expense.getCreatedBy().getUserId();
            expense.getCreatedBy().getUsername();
        }

        if (expense.getVoidedBy() != null) {
            expense.getVoidedBy().getUserId();
            expense.getVoidedBy().getUsername();
        }

        if (expense.getPeriod() != null) {
            expense.getPeriod().getPeriodId();
            expense.getPeriod().getName();
            expense.getPeriod().getIsLocked();
        }

        if (expense.getCategory() != null) {
            expense.getCategory().getCategoryId();
            expense.getCategory().getCategoryName();
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private List<AuditFieldChangeDto> buildCreateChanges(Expense expense) {
        return List.of(
            new AuditFieldChangeDto("expenseNumber", "", safe(expense.getExpenseNumber())),
            new AuditFieldChangeDto("expenseDate", "", String.valueOf(expense.getExpenseDate())),
            new AuditFieldChangeDto("description", "", safe(expense.getDescription())),
            new AuditFieldChangeDto("payeeName", "", safe(expense.getPayeeName())),
            new AuditFieldChangeDto("category", "",
                expense.getCategory() != null ? safe(expense.getCategory().getCategoryName()) : ""),
            new AuditFieldChangeDto("amount", "", stringifyNumber(expense.getAmount())),
            new AuditFieldChangeDto("isVoided", "", String.valueOf(expense.getIsVoided()))
        );
    }
}