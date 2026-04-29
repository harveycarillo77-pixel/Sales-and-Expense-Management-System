package com.example.sales_expense_system.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.sales_expense_system.dto.request.AccountingPeriodRequest;
import com.example.sales_expense_system.model.AccountingPeriod;
import com.example.sales_expense_system.model.User;
import com.example.sales_expense_system.repository.AccountingPeriodRepository;
import com.example.sales_expense_system.repository.UserRepository;
import com.example.sales_expense_system.service.AccountingPeriodService;
import com.example.sales_expense_system.service.AuditLogService;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AccountingPeriodServiceImpl implements AccountingPeriodService {

    private final AccountingPeriodRepository accountingPeriodRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public AccountingPeriodServiceImpl(AccountingPeriodRepository accountingPeriodRepository,
                                       UserRepository userRepository,
                                       AuditLogService auditLogService) {
        this.accountingPeriodRepository = accountingPeriodRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountingPeriod> getAllPeriods() {
        return accountingPeriodRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public AccountingPeriod getPeriodById(Long id) {
        return accountingPeriodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Accounting period not found"));
    }

    @Override
    @Transactional
    public AccountingPeriod createPeriod(AccountingPeriodRequest request, HttpServletRequest httpRequest) {
        validateDates(request);
        validateNoOverlap(request.getStartDate(), request.getEndDate(), null);

        AccountingPeriod period = new AccountingPeriod();
        period.setName(request.getName().trim());
        period.setStartDate(request.getStartDate());
        period.setEndDate(request.getEndDate());
        period.setIsLocked(false);
        period.setLockedBy(null);
        period.setLockedAt(null);

        AccountingPeriod savedPeriod = accountingPeriodRepository.save(period);

        auditLogService.log(
                "CREATE ACCOUNTING PERIOD",
                "Accounting Periods",
                "accounting_periods",
                savedPeriod.getPeriodId(),
                savedPeriod.getName(),
                "Created accounting period " + savedPeriod.getName() + ".",
                "New accounting period was added.",
                null,
                periodSnapshot(savedPeriod),
                """
                [
                  {"field":"name","oldValue":"","newValue":"%s"},
                  {"field":"startDate","oldValue":"","newValue":"%s"},
                  {"field":"endDate","oldValue":"","newValue":"%s"},
                  {"field":"isLocked","oldValue":"","newValue":"false"}
                ]
                """.formatted(
                        escapeJson(savedPeriod.getName()),
                        String.valueOf(savedPeriod.getStartDate()),
                        String.valueOf(savedPeriod.getEndDate())
                ).replace("\n", "").replace("  ", ""),
                getCurrentUser(),
                httpRequest
        );

        return savedPeriod;
    }

    @Override
    @Transactional
    public AccountingPeriod updatePeriod(Long id, AccountingPeriodRequest request, HttpServletRequest httpRequest) {
        AccountingPeriod existing = getPeriodById(id);

        if (Boolean.TRUE.equals(existing.getIsLocked())) {
            throw new RuntimeException("Locked accounting period cannot be updated");
        }

        String oldName = existing.getName();
        String oldStartDate = String.valueOf(existing.getStartDate());
        String oldEndDate = String.valueOf(existing.getEndDate());
        String oldValues = periodSnapshot(existing);

        validateDates(request);
        validateNoOverlap(request.getStartDate(), request.getEndDate(), id);

        existing.setName(request.getName().trim());
        existing.setStartDate(request.getStartDate());
        existing.setEndDate(request.getEndDate());

        List<String> changes = new ArrayList<>();
        addChangedField(changes, "name", oldName, existing.getName());
        addChangedField(changes, "startDate", oldStartDate, String.valueOf(existing.getStartDate()));
        addChangedField(changes, "endDate", oldEndDate, String.valueOf(existing.getEndDate()));

        AccountingPeriod savedPeriod = accountingPeriodRepository.save(existing);

        auditLogService.log(
                "UPDATE ACCOUNTING PERIOD",
                "Accounting Periods",
                "accounting_periods",
                savedPeriod.getPeriodId(),
                savedPeriod.getName(),
                "Updated accounting period " + savedPeriod.getName() + ".",
                changes.isEmpty()
                        ? "Accounting period was saved with no detected field changes."
                        : "Accounting period details were updated.",
                oldValues,
                periodSnapshot(savedPeriod),
                buildChangedFieldsJson(changes),
                getCurrentUser(),
                httpRequest
        );

        return savedPeriod;
    }

    @Override
    @Transactional
    public AccountingPeriod lockPeriod(Long id, HttpServletRequest httpRequest) {
        AccountingPeriod period = getPeriodById(id);

        if (Boolean.TRUE.equals(period.getIsLocked())) {
            throw new RuntimeException("Accounting period is already locked");
        }

        String oldValues = periodSnapshot(period);
        User currentUser = getCurrentUser();

        period.setIsLocked(true);
        period.setLockedBy(currentUser);
        period.setLockedAt(LocalDateTime.now());

        AccountingPeriod savedPeriod = accountingPeriodRepository.save(period);

        auditLogService.log(
                "LOCK ACCOUNTING PERIOD",
                "Accounting Periods",
                "accounting_periods",
                savedPeriod.getPeriodId(),
                savedPeriod.getName(),
                "Locked accounting period " + savedPeriod.getName() + ".",
                "Transactions within this period are now restricted.",
                oldValues,
                periodSnapshot(savedPeriod),
                """
                [
                  {"field":"isLocked","oldValue":"false","newValue":"true"},
                  {"field":"lockedBy","oldValue":"","newValue":"%s"},
                  {"field":"lockedAt","oldValue":"","newValue":"%s"}
                ]
                """.formatted(
                        escapeJson(currentUser.getUsername()),
                        String.valueOf(savedPeriod.getLockedAt())
                ).replace("\n", "").replace("  ", ""),
                currentUser,
                httpRequest
        );

        return savedPeriod;
    }

    @Override
    @Transactional
    public AccountingPeriod unlockPeriod(Long id, HttpServletRequest httpRequest) {
        AccountingPeriod period = getPeriodById(id);

        if (!Boolean.TRUE.equals(period.getIsLocked())) {
            throw new RuntimeException("Accounting period is already unlocked");
        }

        String oldLockedBy = period.getLockedBy() != null ? period.getLockedBy().getUsername() : "";
        String oldLockedAt = period.getLockedAt() != null ? period.getLockedAt().toString() : "";
        String oldValues = periodSnapshot(period);

        period.setIsLocked(false);
        period.setLockedBy(null);
        period.setLockedAt(null);

        AccountingPeriod savedPeriod = accountingPeriodRepository.save(period);

        auditLogService.log(
                "UNLOCK ACCOUNTING PERIOD",
                "Accounting Periods",
                "accounting_periods",
                savedPeriod.getPeriodId(),
                savedPeriod.getName(),
                "Unlocked accounting period " + savedPeriod.getName() + ".",
                "Transactions within this period can now be edited again.",
                oldValues,
                periodSnapshot(savedPeriod),
                """
                [
                  {"field":"isLocked","oldValue":"true","newValue":"false"},
                  {"field":"lockedBy","oldValue":"%s","newValue":""},
                  {"field":"lockedAt","oldValue":"%s","newValue":""}
                ]
                """.formatted(
                        escapeJson(oldLockedBy),
                        escapeJson(oldLockedAt)
                ).replace("\n", "").replace("  ", ""),
                getCurrentUser(),
                httpRequest
        );

        return savedPeriod;
    }

    @Override
    @Transactional
    public void deletePeriod(Long id, HttpServletRequest httpRequest) {
        AccountingPeriod period = getPeriodById(id);

        if (Boolean.TRUE.equals(period.getIsLocked())) {
            throw new RuntimeException("Locked accounting period cannot be deleted");
        }

        String oldValues = periodSnapshot(period);
        String targetDisplay = period.getName();

        accountingPeriodRepository.delete(period);

        auditLogService.log(
                "DELETE ACCOUNTING PERIOD",
                "Accounting Periods",
                "accounting_periods",
                id,
                targetDisplay,
                "Deleted accounting period " + targetDisplay + ".",
                "Accounting period record was deleted.",
                oldValues,
                null,
                """
                [
                  {"field":"deleted","oldValue":"false","newValue":"true"}
                ]
                """.replace("\n", "").replace("  ", ""),
                getCurrentUser(),
                httpRequest
        );
    }

    private void validateDates(AccountingPeriodRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new RuntimeException("End date must not be before start date");
        }
    }

    private void validateNoOverlap(LocalDate startDate, LocalDate endDate, Long currentPeriodId) {
        List<AccountingPeriod> overlapping = accountingPeriodRepository
                .findAllByStartDateLessThanEqualAndEndDateGreaterThanEqual(endDate, startDate);

        boolean hasConflict = overlapping.stream()
                .anyMatch(period -> currentPeriodId == null || !period.getPeriodId().equals(currentPeriodId));

        if (hasConflict) {
            throw new RuntimeException("Accounting period overlaps with an existing period");
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String periodSnapshot(AccountingPeriod period) {
        return """
            {
              "name":"%s",
              "startDate":"%s",
              "endDate":"%s",
              "isLocked":%s,
              "lockedBy":"%s",
              "lockedAt":"%s"
            }
            """.formatted(
                escapeJson(period.getName()),
                String.valueOf(period.getStartDate()),
                String.valueOf(period.getEndDate()),
                Boolean.TRUE.equals(period.getIsLocked()),
                escapeJson(period.getLockedBy() != null ? period.getLockedBy().getUsername() : ""),
                escapeJson(period.getLockedAt() != null ? period.getLockedAt().toString() : "")
            ).replace("\n", "").replace("  ", "");
    }

    private void addChangedField(List<String> changes, String field, Object oldValue, Object newValue) {
        String oldText = oldValue == null ? "" : String.valueOf(oldValue);
        String newText = newValue == null ? "" : String.valueOf(newValue);

        if (!oldText.equals(newText)) {
            changes.add("""
                {"field":"%s","oldValue":"%s","newValue":"%s"}
                """.formatted(
                    escapeJson(field),
                    escapeJson(oldText),
                    escapeJson(newText)
                ).replace("\n", "").replace("  ", ""));
        }
    }

    private String buildChangedFieldsJson(List<String> changes) {
        return "[" + String.join(",", changes) + "]";
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public void validateDateNotInLockedPeriod(LocalDate transactionDate) {
        boolean locked = accountingPeriodRepository
                .existsByIsLockedTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        transactionDate, transactionDate
                );

        if (locked) {
            throw new RuntimeException(
                    "This transaction date belongs to a locked accounting period."
            );
        }
    }
}