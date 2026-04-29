package com.example.sales_expense_system.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;

    @NotBlank(message = "Category name is required")
    @Column(nullable = false, unique = true, length = 100)
    private String categoryName;

    @Column(length = 255)
    private String description;

    @NotNull(message = "Category type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CategoryType type;

    public enum CategoryType {
        SALES, EXPENSE
    }
}