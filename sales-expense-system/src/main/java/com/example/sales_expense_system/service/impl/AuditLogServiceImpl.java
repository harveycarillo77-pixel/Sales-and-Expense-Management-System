package com.example.sales_expense_system.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.sales_expense_system.model.AuditLog;
import com.example.sales_expense_system.model.User;
import com.example.sales_expense_system.repository.AuditLogRepository;
import com.example.sales_expense_system.service.AuditLogService;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public void log(String action, String tableName, Long recordId, User user, HttpServletRequest request) {
        log(
                action,
                tableName,
                tableName,
                recordId,
                null,
                null,
                null,
                null,
                null,
                null,
                user,
                request
        );
    }

    @Override
    @Transactional
    public void log(
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
    ) {
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction(action);
        log.setModuleName(trimToNull(moduleName));
        log.setTableName(tableName);
        log.setRecordId(recordId);
        log.setTargetDisplay(trimToNull(targetDisplay));
        log.setSummary(trimToNull(summary));
        log.setMessage(trimToNull(message));
        log.setOldValues(trimToNull(oldValues));
        log.setNewValues(trimToNull(newValues));
        log.setChangedFields(trimToNull(changedFields));

        if (request != null) {
            String deviceName = trimToNull(request.getHeader("X-Device-Name"));
            String macAddress = trimToNull(request.getHeader("X-Mac-Address"));
            String localIp = trimToNull(request.getHeader("X-Local-IP"));
            String userAgent = trimToNull(request.getHeader("User-Agent"));

            log.setDeviceName(deviceName != null ? deviceName : userAgent);
            log.setMacAddress(macAddress);
            log.setIpAddress(localIp != null ? localIp : resolveClientIp(request));
        }

        auditLogRepository.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAllByOrderByActionTimestampDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> searchLogs(
            Long userId,
            String username,
            String action,
            String tableName,
            String moduleName,
            String targetDisplay,
            String summary,
            String dateFrom,
            String dateTo
    ) {
        LocalDateTime fromDateTime = parseStartDate(dateFrom);
        LocalDateTime toDateTime = parseEndDate(dateTo);

        return auditLogRepository.searchLogs(
                userId,
                trimToNull(username),
                trimToNull(action),
                trimToNull(tableName),
                trimToNull(moduleName),
                trimToNull(targetDisplay),
                trimToNull(summary),
                fromDateTime,
                toDateTime
        );
    }

    private LocalDateTime parseStartDate(String value) {
        try {
            return value == null || value.isBlank()
                    ? null
                    : LocalDate.parse(value.trim()).atStartOfDay();
        } catch (Exception ex) {
            return null;
        }
    }

    private LocalDateTime parseEndDate(String value) {
        try {
            return value == null || value.isBlank()
                    ? null
                    : LocalDate.parse(value.trim()).atTime(23, 59, 59);
        } catch (Exception ex) {
            return null;
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = trimToNull(request.getHeader("X-Forwarded-For"));
        if (forwardedFor != null) {
            int commaIndex = forwardedFor.indexOf(",");
            return commaIndex >= 0 ? forwardedFor.substring(0, commaIndex).trim() : forwardedFor;
        }

        String realIp = trimToNull(request.getHeader("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }

        return request.getRemoteAddr();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}