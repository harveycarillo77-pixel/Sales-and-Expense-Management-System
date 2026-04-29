package com.example.sales_expense_system.dto.request;

import jakarta.validation.constraints.NotBlank;

public class AdminResetPasswordRequest {

    @NotBlank(message = "New password is required")
    private String newPassword;

    public AdminResetPasswordRequest() {
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}