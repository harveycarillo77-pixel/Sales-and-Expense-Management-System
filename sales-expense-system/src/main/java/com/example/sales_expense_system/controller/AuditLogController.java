package com.example.sales_expense_system.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.sales_expense_system.dto.AuditLogDto;
import com.example.sales_expense_system.model.AuditLog;
import com.example.sales_expense_system.service.AuditLogService;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLogDto>> getLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String tableName,
            @RequestParam(required = false) String moduleName,
            @RequestParam(required = false) String targetDisplay,
            @RequestParam(required = false) String summary,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo
    ) {
        List<AuditLogDto> response = auditLogService.searchLogs(
                        userId,
                        username,
                        action,
                        tableName,
                        moduleName,
                        targetDisplay,
                        summary,
                        dateFrom,
                        dateTo
                )
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    private AuditLogDto mapToDto(AuditLog log) {
        AuditLogDto dto = new AuditLogDto();
        dto.setLogId(log.getLogId());
        dto.setUserId(log.getUser() != null ? log.getUser().getUserId() : null);
        dto.setUsername(log.getUser() != null ? log.getUser().getUsername() : null);
        dto.setAction(log.getAction());
        dto.setTableName(log.getTableName());
        dto.setRecordId(log.getRecordId());

        dto.setModuleName(log.getModuleName());
        dto.setTargetDisplay(log.getTargetDisplay());
        dto.setSummary(log.getSummary());
        dto.setMessage(log.getMessage());
        dto.setOldValues(log.getOldValues());
        dto.setNewValues(log.getNewValues());
        dto.setChangedFields(log.getChangedFields());

        dto.setActionTimestamp(log.getActionTimestamp());
        dto.setDeviceName(log.getDeviceName());
        dto.setIpAddress(log.getIpAddress());
        dto.setMacAddress(log.getMacAddress());
        return dto;
    }
}