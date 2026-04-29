package com.example.sales_expense_system.model;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "accounting_periods")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountingPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long periodId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;
    
    @Builder.Default
    @Column(nullable = false)
    private Boolean isLocked = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "locked_by")
    private User lockedBy;

    private LocalDateTime lockedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
}
