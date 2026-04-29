package com.example.sales_expense_system.service.impl;

import java.util.List;
import java.util.ArrayList;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.sales_expense_system.dto.request.AdminCreateUserRequest;
import com.example.sales_expense_system.dto.request.AdminResetPasswordRequest;
import com.example.sales_expense_system.dto.request.AdminUpdateUserRequest;
import com.example.sales_expense_system.model.Role;
import com.example.sales_expense_system.model.User;
import com.example.sales_expense_system.repository.RoleRepository;
import com.example.sales_expense_system.repository.UserRepository;
import com.example.sales_expense_system.service.AdminUserManagementService;
import com.example.sales_expense_system.service.AuditLogService;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AdminUserManagementServiceImpl implements AdminUserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public AdminUserManagementServiceImpl(UserRepository userRepository,
                                          RoleRepository roleRepository,
                                          PasswordEncoder passwordEncoder,
                                          AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findWithRoleByUserId(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    @Transactional
    public User createUser(AdminCreateUserRequest request, HttpServletRequest httpRequest) {
        String username = request.getUsername().trim();

        if (username.isEmpty()) {
            throw new RuntimeException("Username must not be blank");
        }

        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }

        if (request.getPassword() == null || request.getPassword().trim().length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters");
        }

        Role role = roleRepository.findByRoleName(request.getRoleName().trim().toUpperCase())
                .orElseThrow(() -> new RuntimeException("Role not found"));

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setStatus(User.UserStatus.ACTIVE);
        user.setInactivityTimeout(request.getInactivityTimeout());
        user.setTwoFactorEnabled(false);
        user.setTotpSecret(null);

        User savedUser = userRepository.save(user);

        auditLogService.log(
        "CREATE USER",
        "Users",
        "users",
        savedUser.getUserId(),
        savedUser.getUsername(),
        "Admin created user " + savedUser.getUsername() + ".",
        "New user account was created.",
        null,
        userSnapshot(savedUser),
        """
        [
          {"field":"username","oldValue":"","newValue":"%s"},
          {"field":"roleName","oldValue":"","newValue":"%s"},
          {"field":"status","oldValue":"","newValue":"%s"},
          {"field":"inactivityTimeout","oldValue":"","newValue":"%s"},
          {"field":"twoFactorEnabled","oldValue":"","newValue":"%s"}
        ]
        """.formatted(
            escapeJson(savedUser.getUsername()),
            escapeJson(savedUser.getRole() != null ? savedUser.getRole().getRoleName() : ""),
            escapeJson(savedUser.getStatus() != null ? savedUser.getStatus().name() : ""),
            String.valueOf(savedUser.getInactivityTimeout()),
            String.valueOf(savedUser.getTwoFactorEnabled())
        ).replace("\n", "").replace("  ", ""),
        getCurrentAdmin(),
        httpRequest
);

        return savedUser;
    }

    @Override
    @Transactional
    public User updateUser(Long id, AdminUpdateUserRequest request, HttpServletRequest httpRequest) {
        User user = getUserById(id);
        String oldUsername = user.getUsername();
        String oldRoleName = user.getRole() != null ? user.getRole().getRoleName() : "";
        String oldStatus = user.getStatus() != null ? user.getStatus().name() : "";
        Integer oldInactivityTimeout = user.getInactivityTimeout();
        String oldValues = userSnapshot(user);

        if (request.getUsername() != null) {
            String username = request.getUsername().trim();

            if (username.isEmpty()) {
                throw new RuntimeException("Username must not be blank");
            }

            if (!username.equalsIgnoreCase(user.getUsername()) && userRepository.existsByUsername(username)) {
                throw new RuntimeException("Username already exists");
            }

            user.setUsername(username);
        }

        if (request.getRoleName() != null) {
            Role role = roleRepository.findByRoleName(request.getRoleName().trim().toUpperCase())
                    .orElseThrow(() -> new RuntimeException("Role not found"));
            user.setRole(role);
        }

        if (request.getStatus() != null) {
            String status = request.getStatus().trim().toUpperCase();

            try {
                user.setStatus(User.UserStatus.valueOf(status));
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException("Invalid user status");
            }
        }

        if (request.getInactivityTimeout() != null) {
            user.setInactivityTimeout(request.getInactivityTimeout());
        }

        User savedUser = userRepository.save(user);
        List<String> changes = new ArrayList<>();
        addChangedField(changes, "username", oldUsername, user.getUsername());
        addChangedField(changes, "roleName", oldRoleName, user.getRole() != null ? user.getRole().getRoleName() : "");
        addChangedField(changes, "status", oldStatus, user.getStatus() != null ? user.getStatus().name() : "");
        addChangedField(changes, "inactivityTimeout", oldInactivityTimeout, user.getInactivityTimeout());

        auditLogService.log(
        "UPDATE USER",
        "Users",
        "users",
        savedUser.getUserId(),
        savedUser.getUsername(),
        "Admin updated user " + savedUser.getUsername() + ".",
        changes.isEmpty() ? "User record was saved with no detected field changes." : "User account details were updated.",
        oldValues,
        userSnapshot(savedUser),
        buildChangedFieldsJson(changes),
        getCurrentAdmin(),
        httpRequest
);

        return savedUser;
    }

    @Override
    @Transactional
    public User activateUser(Long id, HttpServletRequest httpRequest) {
        User user = getUserById(id);
        user.setStatus(User.UserStatus.ACTIVE);

        User savedUser = userRepository.save(user);

        auditLogService.log(
        "ACTIVATE USER",
        "Users",
        "users",
        savedUser.getUserId(),
        savedUser.getUsername(),
        "Admin activated user " + savedUser.getUsername() + ".",
        "User account status changed to ACTIVE.",
        """
        {"status":"INACTIVE"}
        """.replace("\n", "").replace("  ", ""),
        """
        {"status":"ACTIVE"}
        """.replace("\n", "").replace("  ", ""),
        """
        [{"field":"status","oldValue":"INACTIVE","newValue":"ACTIVE"}]
        """.replace("\n", "").replace("  ", ""),
        getCurrentAdmin(),
        httpRequest
);

        return savedUser;
    }

    @Override
    @Transactional
    public User deactivateUser(Long id, HttpServletRequest httpRequest) {
        User user = getUserById(id);
        user.setStatus(User.UserStatus.INACTIVE);

        User savedUser = userRepository.save(user);

        auditLogService.log(
        "DEACTIVATE USER",
        "Users",
        "users",
        savedUser.getUserId(),
        savedUser.getUsername(),
        "Admin deactivated user " + savedUser.getUsername() + ".",
        "User account status changed to INACTIVE.",
        """
        {"status":"ACTIVE"}
        """.replace("\n", "").replace("  ", ""),
        """
        {"status":"INACTIVE"}
        """.replace("\n", "").replace("  ", ""),
        """
        [{"field":"status","oldValue":"ACTIVE","newValue":"INACTIVE"}]
        """.replace("\n", "").replace("  ", ""),
        getCurrentAdmin(),
        httpRequest
);
        return savedUser;
    }

    @Override
    @Transactional
    public User resetPassword(Long id, AdminResetPasswordRequest request, HttpServletRequest httpRequest) {
        User user = getUserById(id);

        if (request.getNewPassword() == null || request.getNewPassword().trim().length() < 6) {
            throw new RuntimeException("New password must be at least 6 characters");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        User savedUser = userRepository.save(user);

        auditLogService.log(
        "ADMIN RESET PASSWORD",
        "Users",
        "users",
        savedUser.getUserId(),
        savedUser.getUsername(),
        "Admin reset password for user " + savedUser.getUsername() + ".",
        "Password hash was updated by an administrator.",
        null,
        null,
        """
        [{"field":"password","oldValue":"[HIDDEN]","newValue":"[HIDDEN]"}]
        """.replace("\n", "").replace("  ", ""),
        getCurrentAdmin(),
        httpRequest
);

        return savedUser;
    }

    @Override
    @Transactional
    public User resetTwoFactor(Long id, HttpServletRequest httpRequest) {
        User user = getUserById(id);
        user.setTwoFactorEnabled(false);
        user.setTotpSecret(null);

        User savedUser = userRepository.save(user);

        auditLogService.log(
        "ADMIN RESET 2FA",
        "Users",
        "users",
        savedUser.getUserId(),
        savedUser.getUsername(),
        "Admin reset 2FA for user " + savedUser.getUsername() + ".",
        "Two-factor authentication was reset and TOTP secret was cleared.",
        """
        {"twoFactorEnabled":"true"}
        """.replace("\n", "").replace("  ", ""),
        """
        {"twoFactorEnabled":"false"}
        """.replace("\n", "").replace("  ", ""),
        """
        [{"field":"twoFactorEnabled","oldValue":"true","newValue":"false"}]
        """.replace("\n", "").replace("  ", ""),
        getCurrentAdmin(),
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

    private User getCurrentAdmin() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepository.findWithRoleByUsername(username)
                .orElseThrow(() -> new RuntimeException("Authenticated admin not found"));
    }
}