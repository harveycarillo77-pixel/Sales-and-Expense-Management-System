package com.example.sales_expense_system.service;

import com.example.sales_expense_system.dto.request.ChangePasswordRequest;
import com.example.sales_expense_system.dto.request.UpdateInactivityTimeoutRequest;
import com.example.sales_expense_system.model.User;

import jakarta.servlet.http.HttpServletRequest;

public interface AccountSettingsService {

    User getCurrentUserSettings();

    User changePassword(ChangePasswordRequest request, HttpServletRequest httpRequest);

    User updateInactivityTimeout(UpdateInactivityTimeoutRequest request, HttpServletRequest httpRequest);

    User resetTwoFactor(HttpServletRequest httpRequest);

    User updateOwnUsername(String newUsername, HttpServletRequest httpRequest);

    void verifyCurrentPassword(String username, String password);
}