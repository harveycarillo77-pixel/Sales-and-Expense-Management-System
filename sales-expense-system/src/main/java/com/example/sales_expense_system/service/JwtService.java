package com.example.sales_expense_system.service;

import org.springframework.security.core.userdetails.UserDetails;

import com.example.sales_expense_system.model.User;

public interface JwtService {

    String generateToken(User user);

    boolean validateToken(String token, User user);

    String extractUsername(String token);

    boolean isTokenValid(String token, UserDetails userDetails);
    
}