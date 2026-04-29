package com.example.sales_expense_system.service.impl;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.sales_expense_system.dto.request.ChangePasswordRequest;
import com.example.sales_expense_system.dto.request.UpdateInactivityTimeoutRequest;
import com.example.sales_expense_system.dto.request.VerifyPasswordRequest;
import com.example.sales_expense_system.model.User;
import com.example.sales_expense_system.repository.UserRepository;
import com.example.sales_expense_system.service.AccountSettingsService;
import com.example.sales_expense_system.service.AuditLogService;

import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;

@Service
public class AccountSettingsServiceImpl implements AccountSettingsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public AccountSettingsServiceImpl(UserRepository userRepository,
                                      PasswordEncoder passwordEncoder,
                                      AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public User getCurrentUserSettings() {
        return getCurrentUser();
    }

    @Override
    @Transactional
    public User changePassword(ChangePasswordRequest request, HttpServletRequest httpRequest) {
        User currentUser = getCurrentUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("New password and confirm password do not match");
        }

        if (request.getNewPassword().trim().length() < 6) {
            throw new RuntimeException("New password must be at least 6 characters");
        }

        currentUser.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
User savedUser = userRepository.save(currentUser);

auditLogService.log(
        "CHANGE PASSWORD",
        "Users",
        "users",
        savedUser.getUserId(),
        savedUser.getUsername(),
        "User changed their own password.",
        "Password hash was updated by the account owner.",
        null,
        null,
        """
        [{"field":"password","oldValue":"[HIDDEN]","newValue":"[HIDDEN]"}]
        """.replace("\n", "").replace("  ", ""),
        currentUser,
        httpRequest
);

return savedUser;
    }

    @Override
@Transactional
public User updateOwnUsername(String newUsername, HttpServletRequest httpRequest) {
    User currentUser = getCurrentUser();

    if (newUsername == null || newUsername.trim().isEmpty()) {
        throw new RuntimeException("Username must not be blank");
    }

    String trimmedUsername = newUsername.trim();

    if (!trimmedUsername.equalsIgnoreCase(currentUser.getUsername())
            && userRepository.existsByUsername(trimmedUsername)) {
        throw new RuntimeException("Username already exists");
    }

    String oldUsername = currentUser.getUsername();
    String oldValues = userSnapshot(currentUser);

    currentUser.setUsername(trimmedUsername);
    User savedUser = userRepository.save(currentUser);

    List<String> changes = new ArrayList<>();
    addChangedField(changes, "username", oldUsername, savedUser.getUsername());

    auditLogService.log(
            "UPDATE USERNAME",
            "Users",
            "users",
            savedUser.getUserId(),
            savedUser.getUsername(),
            "User updated their own username from " + oldUsername + " to " + savedUser.getUsername() + ".",
            "Own account username was changed.",
            oldValues,
            userSnapshot(savedUser),
            buildChangedFieldsJson(changes),
            currentUser,
            httpRequest
    );

    return savedUser;
}

    @Override
    @Transactional
    public User updateInactivityTimeout(UpdateInactivityTimeoutRequest request, HttpServletRequest httpRequest) {
        User currentUser = getCurrentUser();

        Integer oldInactivityTimeout = currentUser.getInactivityTimeout();
String oldValues = userSnapshot(currentUser);

currentUser.setInactivityTimeout(request.getInactivityTimeout());
User savedUser = userRepository.save(currentUser);

List<String> changes = new ArrayList<>();
addChangedField(changes, "inactivityTimeout", oldInactivityTimeout, savedUser.getInactivityTimeout());

auditLogService.log(
        "UPDATE INACTIVITY TIMEOUT",
        "Users",
        "users",
        savedUser.getUserId(),
        savedUser.getUsername(),
        "User updated their inactivity timeout.",
        "Account session timeout setting was changed.",
        oldValues,
        userSnapshot(savedUser),
        buildChangedFieldsJson(changes),
        currentUser,
        httpRequest
);

return savedUser;
    }

    @Override
    @Transactional
    public User resetTwoFactor(HttpServletRequest httpRequest) {
        User currentUser = getCurrentUser();

        String oldValues = userSnapshot(currentUser);

currentUser.setTwoFactorEnabled(false);
currentUser.setTotpSecret(null);

User savedUser = userRepository.save(currentUser);

auditLogService.log(
        "RESET 2FA",
        "Users",
        "users",
        savedUser.getUserId(),
        savedUser.getUsername(),
        "User reset their own 2FA.",
        "Two-factor authentication was reset and TOTP secret was cleared.",
        oldValues,
        userSnapshot(savedUser),
        """
        [{"field":"twoFactorEnabled","oldValue":"true","newValue":"false"}]
        """.replace("\n", "").replace("  ", ""),
        currentUser,
        httpRequest
);

return savedUser;
    }

    private String escapeJson(String value) {
    if (value == null) return "";
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
}

private String userSnapshot(User user) {
    return """
        {
          "username":"%s",
          "roleName":"%s",
          "status":"%s",
          "inactivityTimeout":%d,
          "twoFactorEnabled":%s
        }
        """.formatted(
            escapeJson(user.getUsername()),
            escapeJson(user.getRole() != null ? user.getRole().getRoleName() : ""),
            escapeJson(user.getStatus() != null ? user.getStatus().name() : ""),
            user.getInactivityTimeout() != null ? user.getInactivityTimeout() : 0,
            user.getTwoFactorEnabled()
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

        return userRepository.findWithRoleByUsername(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

     @Override
    public void verifyCurrentPassword(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found."));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Incorrect password.");
        }
    }
}