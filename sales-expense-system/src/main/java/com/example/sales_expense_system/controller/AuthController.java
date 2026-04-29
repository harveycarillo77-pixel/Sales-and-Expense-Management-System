package com.example.sales_expense_system.controller;

import com.example.sales_expense_system.dto.request.LoginRequest;
import com.example.sales_expense_system.dto.request.OtpRequest;
import com.example.sales_expense_system.dto.response.JwtResponse;
import com.example.sales_expense_system.dto.response.LoginResponse;
import com.example.sales_expense_system.dto.response.TotpResponse;
import com.example.sales_expense_system.dto.request.RegisterRequest;
import com.example.sales_expense_system.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/register")
    public ResponseEntity<TotpResponse> register(@RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        TotpResponse response = authService.register(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        LoginResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<JwtResponse> verifyOtp(@RequestBody OtpRequest request, HttpServletRequest httpRequest) {
        JwtResponse response = authService.verifyOtp(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(Authentication authentication,
                                HttpServletRequest httpRequest) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("No authenticated user found for logout");
        }

        authService.logout(authentication.getName(), httpRequest);
        return ResponseEntity.ok("Logged out successfully");
    }

    @GetMapping("/session-expired")
    public ResponseEntity<Void> sessionExpired() {
        // JwtAuthFilter handles the logging in the ExpiredJwtException catch block.
        // This endpoint only needs to exist and be reachable — it never executes
        // for an expired token because the filter intercepts it first.
        return ResponseEntity.ok().build();
    }
}