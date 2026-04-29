package com.example.sales_expense_system.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.sales_expense_system.dto.UserManagementDto;
import com.example.sales_expense_system.dto.request.AdminCreateUserRequest;
import com.example.sales_expense_system.dto.request.AdminResetPasswordRequest;
import com.example.sales_expense_system.dto.request.AdminUpdateUserRequest;
import com.example.sales_expense_system.model.User;
import com.example.sales_expense_system.service.AdminUserManagementService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserManagementController {

    private final AdminUserManagementService adminUserManagementService;

    public AdminUserManagementController(AdminUserManagementService adminUserManagementService) {
        this.adminUserManagementService = adminUserManagementService;
    }

    @GetMapping
    public ResponseEntity<List<UserManagementDto>> getAllUsers() {
        List<UserManagementDto> response = adminUserManagementService.getAllUsers()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserManagementDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(mapToDto(adminUserManagementService.getUserById(id)));
    }

    @PostMapping
    public ResponseEntity<UserManagementDto> createUser(@Valid @RequestBody AdminCreateUserRequest request,
                                                        HttpServletRequest httpRequest) {
        return ResponseEntity.ok(mapToDto(adminUserManagementService.createUser(request, httpRequest)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserManagementDto> updateUser(@PathVariable Long id,
                                                        @Valid @RequestBody AdminUpdateUserRequest request,
                                                        HttpServletRequest httpRequest) {
        return ResponseEntity.ok(mapToDto(adminUserManagementService.updateUser(id, request, httpRequest)));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<UserManagementDto> activateUser(@PathVariable Long id,
                                                          HttpServletRequest httpRequest) {
        return ResponseEntity.ok(mapToDto(adminUserManagementService.activateUser(id, httpRequest)));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<UserManagementDto> deactivateUser(@PathVariable Long id,
                                                            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(mapToDto(adminUserManagementService.deactivateUser(id, httpRequest)));
    }

    @PutMapping("/{id}/reset-password")
    public ResponseEntity<UserManagementDto> resetPassword(@PathVariable Long id,
                                                           @Valid @RequestBody AdminResetPasswordRequest request,
                                                           HttpServletRequest httpRequest) {
        return ResponseEntity.ok(mapToDto(adminUserManagementService.resetPassword(id, request, httpRequest)));
    }

    @PutMapping("/{id}/reset-2fa")
    public ResponseEntity<UserManagementDto> resetTwoFactor(@PathVariable Long id,
                                                            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(mapToDto(adminUserManagementService.resetTwoFactor(id, httpRequest)));
    }

    private UserManagementDto mapToDto(User user) {
        UserManagementDto dto = new UserManagementDto();
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setRoleName(user.getRole() != null ? user.getRole().getRoleName() : null);
        dto.setStatus(user.getStatus() != null ? user.getStatus().name() : null);
        dto.setTwoFactorEnabled(user.getTwoFactorEnabled());
        dto.setInactivityTimeout(user.getInactivityTimeout());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}