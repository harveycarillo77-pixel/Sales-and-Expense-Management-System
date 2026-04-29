package com.example.sales_expense_system.service;

import com.example.sales_expense_system.dto.response.LoginResponse;
import com.example.sales_expense_system.dto.request.LoginRequest;
import com.example.sales_expense_system.dto.request.OtpRequest;
import com.example.sales_expense_system.dto.request.RegisterRequest;
import com.example.sales_expense_system.dto.request.VerifyPasswordRequest;
import com.example.sales_expense_system.dto.response.TotpResponse;
import com.example.sales_expense_system.dto.response.JwtResponse;
import com.example.sales_expense_system.model.User;

import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    TotpResponse register(RegisterRequest request, HttpServletRequest httpRequest);

    LoginResponse login(LoginRequest request, HttpServletRequest httpRequest);

    JwtResponse verifyOtp(OtpRequest request, HttpServletRequest httpRequest); 

    void logout(String username, HttpServletRequest httpRequest);
    
    User getProfile(String username);
    
}
