package com.example.sales_expense_system.service.impl;

import com.example.sales_expense_system.dto.request.LoginRequest;
import com.example.sales_expense_system.dto.request.OtpRequest;
import com.example.sales_expense_system.dto.request.RegisterRequest;
import com.example.sales_expense_system.dto.response.JwtResponse;
import com.example.sales_expense_system.dto.response.LoginResponse;
import com.example.sales_expense_system.dto.response.TotpResponse;
import com.example.sales_expense_system.model.Role;
import com.example.sales_expense_system.model.User;
import com.example.sales_expense_system.model.User.UserStatus;
import com.example.sales_expense_system.repository.RoleRepository;
import com.example.sales_expense_system.repository.UserRepository;
import com.example.sales_expense_system.service.AuthService;
import com.example.sales_expense_system.service.AuditLogService;
import com.example.sales_expense_system.service.JwtService;
import com.example.sales_expense_system.util.AuditActions;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.QrData;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Base64;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CodeVerifier totpService;
    private final QrGenerator qrGenerator;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;

    public AuthServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           CodeVerifier totpService,
                           QrGenerator qrGenerator,
                           JwtService jwtService,
                           AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.totpService = totpService;
        this.qrGenerator = qrGenerator;
        this.jwtService = jwtService;
        this.auditLogService = auditLogService;
    }

    @Override
    public TotpResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }
        if (request.getPassword().length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters");
        }

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        DefaultSecretGenerator secretGenerator = new DefaultSecretGenerator();
        String totpSecret = secretGenerator.generate();

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setPasswordHash(encodedPassword);
        user.setTotpSecret(totpSecret);
        user.setStatus(UserStatus.ACTIVE);

        Role defaultRole = roleRepository.findByRoleName("ACCOUNTANT")
                .orElseThrow(() -> new RuntimeException("Default role not found"));
        user.setRole(defaultRole);

        user.setTwoFactorEnabled(false);

        User savedUser = userRepository.save(user);

        auditLogService.log(
                "REGISTER",
                "Authentication",
                "users",
                savedUser.getUserId(),
                savedUser.getUsername(),
                "Registered new user " + savedUser.getUsername() + ".",
                "New account was registered and is pending first successful 2FA completion.",
                null,
                authUserSnapshot(savedUser),
                """
                [
                  {"field":"username","oldValue":"","newValue":"%s"},
                  {"field":"roleName","oldValue":"","newValue":"%s"},
                  {"field":"status","oldValue":"","newValue":"%s"},
                  {"field":"twoFactorEnabled","oldValue":"","newValue":"%s"}
                ]
                """.formatted(
                        escapeJson(savedUser.getUsername()),
                        escapeJson(savedUser.getRole() != null ? savedUser.getRole().getRoleName() : ""),
                        escapeJson(savedUser.getStatus() != null ? savedUser.getStatus().name() : ""),
                        String.valueOf(savedUser.getTwoFactorEnabled())
                ).replace("\n", "").replace("  ", ""),
                savedUser,
                null
        );

        QrData qrData = new QrData.Builder()
                .label(savedUser.getUsername())
                .secret(totpSecret)
                .issuer("SalesExpenseSystem")
                .build();

        byte[] qrBytes;

        try {
            qrBytes = qrGenerator.generate(qrData);
        } catch (Exception e) {
            throw new RuntimeException("Error generating QR Code", e);
        }

        String qrCodeUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(qrBytes);

        return new TotpResponse(savedUser.getUsername(), totpSecret, qrCodeUrl);
    }

    @Override
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findWithRoleByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid password");
        }

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new RuntimeException("Your account is inactive");
        }

        if (!user.getTwoFactorEnabled()) {
            if (user.getTotpSecret() == null || user.getTotpSecret().isEmpty()) {
                DefaultSecretGenerator secretGenerator = new DefaultSecretGenerator();
                user.setTotpSecret(secretGenerator.generate());
                userRepository.save(user);
            }

            QrData qrData = new QrData.Builder()
                    .label(user.getUsername())
                    .secret(user.getTotpSecret())
                    .issuer("SalesExpenseSystem")
                    .build();

            byte[] qrBytes;
            try {
                qrBytes = qrGenerator.generate(qrData);
            } catch (Exception e) {
                throw new RuntimeException("Error generating QR Code", e);
            }

            String qrCodeUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(qrBytes);

            return new LoginResponse(
                user.getUserId(),
                user.getUsername(),
                "2FA_SETUP_REQUIRED",
                qrCodeUrl,
                user.getTotpSecret()
            );
        }

        return new LoginResponse(user.getUserId(), user.getUsername(), "OTP_REQUIRED");
    }

    @Override
    public JwtResponse verifyOtp(OtpRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findWithRoleByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean wasTwoFactorEnabled = Boolean.TRUE.equals(user.getTwoFactorEnabled());
        String oldValues = authUserSnapshot(user);

        boolean isValid = totpService.isValidCode(user.getTotpSecret(), request.getOtp());

        if (!isValid) {
            throw new RuntimeException("Invalid OTP");
        }

        if (!user.getTwoFactorEnabled()) {
            user.setTwoFactorEnabled(true);
            user = userRepository.save(user);
        }

        String changedFields = "[]";
        String summary = "User " + user.getUsername() + " logged in successfully.";
        String message = "OTP verification succeeded and login was completed.";

        if (!wasTwoFactorEnabled) {
            changedFields = """
                [
                  {"field":"twoFactorEnabled","oldValue":"false","newValue":"true"}
                ]
                """.replace("\n", "").replace("  ", "");
            summary = "User " + user.getUsername() + " completed first 2FA enrollment and logged in.";
            message = "OTP verification succeeded, 2FA was enabled, and login was completed.";
        }

        auditLogService.log(
                AuditActions.LOGIN,
                "Authentication",
                "auth",
                user.getUserId(),
                user.getUsername(),
                summary,
                message,
                oldValues,
                authUserSnapshot(user),
                changedFields,
                user,
                httpRequest
        );

        String token = jwtService.generateToken(user);

        return new JwtResponse(
                token,
                user.getUserId(),
                user.getUsername(),
                user.getRole() != null ? user.getRole().getRoleName() : null,
                user.getStatus() != null ? user.getStatus().name() : null,
                user.getInactivityTimeout(),
                user.getTwoFactorEnabled()
        );
    }

    @Override
    public void logout(String username, HttpServletRequest httpRequest) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        auditLogService.log(
                AuditActions.LOGOUT,
                "Authentication",
                "auth",
                user.getUserId(),
                user.getUsername(),
                "User " + user.getUsername() + " logged out.",
                "Authenticated session was ended by the user.",
                null,
                null,
                "[]",
                user,
                httpRequest
        );
    }

    @Override
    public User getProfile(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String authUserSnapshot(User user) {
        return """
            {
              "username":"%s",
              "roleName":"%s",
              "status":"%s",
              "twoFactorEnabled":%s
            }
            """.formatted(
                escapeJson(user.getUsername()),
                escapeJson(user.getRole() != null ? user.getRole().getRoleName() : ""),
                escapeJson(user.getStatus() != null ? user.getStatus().name() : ""),
                Boolean.TRUE.equals(user.getTwoFactorEnabled())
            ).replace("\n", "").replace("  ", "");
    }
}