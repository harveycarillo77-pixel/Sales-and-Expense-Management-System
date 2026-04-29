package com.example.sales_expense_system.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long expenseId;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // optional based on your actual expense process
    @Column(name = "or_number", length = 50, unique = true)
    private String expenseNumber;

    @NotNull
    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @NotNull
    @Column(name = "description", length = 255, nullable = false)
    private String description;

    @Column(name = "payee_name", length = 150)
    private String payeeName;

    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Builder.Default
    @Column(name = "is_voided", nullable = false)
    private Boolean isVoided = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voided_by")
    private User voidedBy;

    @Column(name = "voided_at")
    private LocalDateTime voidedAt;

    @Column(name = "void_reason", length = 255)
    private String voidReason;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id", nullable = false)
    private AccountingPeriod period;
}