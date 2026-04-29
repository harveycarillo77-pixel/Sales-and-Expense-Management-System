package com.example.sales_expense_system.dto.request;

public class OtpRequest {
    private String username;
    private String otp;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
}