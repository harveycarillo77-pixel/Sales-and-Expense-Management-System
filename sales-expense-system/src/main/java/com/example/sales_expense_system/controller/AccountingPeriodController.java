package com.example.sales_expense_system.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.sales_expense_system.dto.AccountingPeriodDto;
import com.example.sales_expense_system.dto.request.AccountingPeriodRequest;
import com.example.sales_expense_system.model.AccountingPeriod;
import com.example.sales_expense_system.service.AccountingPeriodService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/periods")
public class AccountingPeriodController {

    private final AccountingPeriodService periodService;

    public AccountingPeriodController(AccountingPeriodService periodService) {
        this.periodService = periodService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<List<AccountingPeriodDto>> getPeriods() {
        List<AccountingPeriodDto> response = periodService.getAllPeriods()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<AccountingPeriodDto> getPeriodById(@PathVariable Long id) {
        return ResponseEntity.ok(mapToDto(periodService.getPeriodById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountingPeriodDto> createPeriod(@Valid @RequestBody AccountingPeriodRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(mapToDto(periodService.createPeriod(request, httpRequest)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountingPeriodDto> updatePeriod(@PathVariable Long id,
                                                            @Valid @RequestBody AccountingPeriodRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(mapToDto(periodService.updatePeriod(id, request, httpRequest)));
    }

    @PutMapping("/{id}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountingPeriodDto> lockPeriod(@PathVariable Long id, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(mapToDto(periodService.lockPeriod(id, httpRequest)));
    }

    @PutMapping("/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountingPeriodDto> unlockPeriod(@PathVariable Long id, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(mapToDto(periodService.unlockPeriod(id, httpRequest)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePeriod(@PathVariable Long id, HttpServletRequest httpRequest) {
        periodService.deletePeriod(id, httpRequest);
        return ResponseEntity.noContent().build();
    }

    private AccountingPeriodDto mapToDto(AccountingPeriod period) {
        AccountingPeriodDto dto = new AccountingPeriodDto();
        dto.setPeriodId(period.getPeriodId());
        dto.setPeriodName(period.getName());
        dto.setStartDate(period.getStartDate());
        dto.setEndDate(period.getEndDate());
        dto.setIsLocked(period.getIsLocked());
        dto.setLockedByUsername(
                period.getLockedBy() != null ? period.getLockedBy().getUsername() : null
        );
        dto.setLockedAt(period.getLockedAt());
        return dto;
    }
}