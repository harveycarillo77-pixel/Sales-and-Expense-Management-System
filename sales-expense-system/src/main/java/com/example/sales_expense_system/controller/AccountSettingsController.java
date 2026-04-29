package com.example.sales_expense_system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import com.example.sales_expense_system.dto.AccountSettingsDto;
import com.example.sales_expense_system.dto.request.ChangePasswordRequest;
import com.example.sales_expense_system.dto.request.UpdateInactivityTimeoutRequest;
import com.example.sales_expense_system.dto.request.VerifyPasswordRequest;
import com.example.sales_expense_system.model.User;
import com.example.sales_expense_system.service.AccountSettingsService;
import com.example.sales_expense_system.service.impl.AccountSettingsServiceImpl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/account")
@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
public class AccountSettingsController {

    private final AccountSettingsService accountSettingsService;

    public AccountSettingsController(AccountSettingsService accountSettingsService) {
        this.accountSettingsService = accountSettingsService;
    }

    @GetMapping("/settings")
    public ResponseEntity<AccountSettingsDto> getSettings() {
        return ResponseEntity.ok(mapToDto(accountSettingsService.getCurrentUserSettings()));
    }

    @PutMapping("/change-password")
    public ResponseEntity<AccountSettingsDto> changePassword(@Valid @RequestBody ChangePasswordRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(mapToDto(accountSettingsService.changePassword(request, httpRequest)));
    }

    @PutMapping("/username")
public ResponseEntity<AccountSettingsDto> updateOwnUsername(
        @RequestBody Map<String, String> body,
        HttpServletRequest httpRequest
) {
    String newUsername = body != null ? body.get("username") : null;
    return ResponseEntity.ok(mapToDto(accountSettingsService.updateOwnUsername(newUsername, httpRequest)));
}

    @PutMapping("/inactivity-timeout")
    public ResponseEntity<AccountSettingsDto> updateInactivityTimeout(
            @Valid @RequestBody UpdateInactivityTimeoutRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(mapToDto(accountSettingsService.updateInactivityTimeout(request, httpRequest)));
    }

    @PostMapping("/verify-password")
    public ResponseEntity<?> verifyPassword(
            @Valid @RequestBody VerifyPasswordRequest request,
            Authentication authentication
    ) {
        accountSettingsService.verifyCurrentPassword(authentication.getName(), request.getPassword());
        return ResponseEntity.ok(Map.of("message", "Password verified."));
    }

    @PutMapping("/reset-2fa")
    public ResponseEntity<AccountSettingsDto> resetTwoFactor(HttpServletRequest httpRequest) {
        return ResponseEntity.ok(mapToDto(accountSettingsService.resetTwoFactor(httpRequest)));
    }

    private AccountSettingsDto mapToDto(User user) {
        AccountSettingsDto dto = new AccountSettingsDto();
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setRoleName(user.getRole() != null ? user.getRole().getRoleName() : null);
        dto.setStatus(user.getStatus() != null ? user.getStatus().name() : null);
        dto.setTwoFactorEnabled(user.getTwoFactorEnabled());
        dto.setInactivityTimeout(user.getInactivityTimeout());
        return dto;
    }
}