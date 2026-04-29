package com.example.sales_expense_system.service;

public interface TotpService {

    boolean verifyCode(String secret, String code);
    
}