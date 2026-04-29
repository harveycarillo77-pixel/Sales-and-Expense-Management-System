package com.example.sales_expense_system.dto.response;

public class LoginResponse {

    private Long userId;
    private String username;
    private String status;
    private String qrCodeUrl;
    private String rawTotpSecret; // shown only during 2FA setup

    public LoginResponse() {}

    public LoginResponse(Long userId, String username, String status) {
        this.userId = userId;
        this.username = username;
        this.status = status;
    }

    public LoginResponse(Long userId, String username, String status, String qrCodeUrl) {
        this.userId = userId;
        this.username = username;
        this.status = status;
        this.qrCodeUrl = qrCodeUrl;
    }

    public LoginResponse(Long userId, String username, String status, String qrCodeUrl, String rawTotpSecret) {
        this.userId = userId;
        this.username = username;
        this.status = status;
        this.qrCodeUrl = qrCodeUrl;
        this.rawTotpSecret = rawTotpSecret;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }

    public String getRawTotpSecret() {
        return rawTotpSecret;
    }

    public void setRawTotpSecret(String rawTotpSecret) {
        this.rawTotpSecret = rawTotpSecret;
    }
}