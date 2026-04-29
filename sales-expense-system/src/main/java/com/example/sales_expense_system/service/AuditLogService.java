package com.example.sales_expense_system.service;

import java.util.List;

import com.example.sales_expense_system.model.AuditLog;
import com.example.sales_expense_system.model.User;

import jakarta.servlet.http.HttpServletRequest;

public interface AuditLogService {

    void log(String action, String tableName, Long recordId, User user, HttpServletRequest request);

    void log(
            String action,
            String moduleName,
            String tableName,
            Long recordId,
            String targetDisplay,
            String summary,
            String message,
            String oldValues,
            String newValues,
            String changedFields,
            User user,
            HttpServletRequest request
    );

    List<AuditLog> getAllLogs();

    List<AuditLog> searchLogs(
            Long userId,
            String username,
            String action,
            String tableName,
            String moduleName,
            String targetDisplay,
            String summary,
            String dateFrom,
            String dateTo
    );
}