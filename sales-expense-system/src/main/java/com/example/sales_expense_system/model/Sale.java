package com.example.sales_expense_system.model;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "sales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long saleId;

    @NotNull
    @Column(name = "or_number", length = 50, unique = true)
    private String saleNumber;

    @NotNull
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @NotNull
    @NotBlank
    @Column(name = "item_name", length = 255, nullable = false)
    private String itemName;

    @Column(name = "customer_name", length = 150)
    private String customerName;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category; // Ensure type='Sales' in service layer or DB trigger

    @NotNull
    @Positive
    @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @NotNull
    @DecimalMin(value = "0.0", message = "Unit price must be greater than or equal to 0")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @NotNull
    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @NotNull
    @Builder.Default
    @Column(name = "is_voided", nullable = false)
    private Boolean isVoided = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voided_by")
    private User voidedBy;

    @Column(name = "voided_at")
    private LocalDateTime voidedAt;

    @Column(name = "void_reason")
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

    @PrePersist
    @PreUpdate
    private void calculateTotalAmount() {
        if (quantity != null && unitPrice != null) {
            totalAmount = quantity.multiply(unitPrice);
        }
    }
                                                                                                                                                                                        
    @Version
    private Long version;
}
