package com.example.sales_expense_system.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String action;

    @Column(name = "table_name", nullable = false, length = 100)
    private String tableName;

    @Column(name = "record_id", nullable = false)
    private Long recordId;

    @Column(name = "module_name", length = 100)
    private String moduleName;

    @Column(name = "target_display", length = 255)
    private String targetDisplay;

    @Column(name = "summary", length = 500)
    private String summary;

    @Column(name = "message", length = 1000)
    private String message;

    @Lob
    @Column(name = "old_values", columnDefinition = "LONGTEXT")
    private String oldValues;

    @Lob
    @Column(name = "new_values", columnDefinition = "LONGTEXT")
    private String newValues;

    @Lob
    @Column(name = "changed_fields", columnDefinition = "LONGTEXT")
    private String changedFields;

    @CreationTimestamp
    @Column(name = "action_timestamp", nullable = false, updatable = false)
    private LocalDateTime actionTimestamp;

    @Column(name = "device_name", length = 255)
    private String deviceName;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "mac_address", length = 50)
    private String macAddress;
}