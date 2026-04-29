package com.example.sales_expense_system.service;

import java.util.List;

import com.example.sales_expense_system.dto.request.AdminCreateUserRequest;
import com.example.sales_expense_system.dto.request.AdminResetPasswordRequest;
import com.example.sales_expense_system.dto.request.AdminUpdateUserRequest;
import com.example.sales_expense_system.model.User;

import jakarta.servlet.http.HttpServletRequest;

public interface AdminUserManagementService {

    List<User> getAllUsers();

    User getUserById(Long id);

    User createUser(AdminCreateUserRequest request, HttpServletRequest httpRequest);

    User updateUser(Long id, AdminUpdateUserRequest request, HttpServletRequest httpRequest);

    User activateUser(Long id, HttpServletRequest httpRequest);

    User deactivateUser(Long id, HttpServletRequest httpRequest);

    User resetPassword(Long id, AdminResetPasswordRequest request, HttpServletRequest httpRequest);

    User resetTwoFactor(Long id, HttpServletRequest httpRequest);
}