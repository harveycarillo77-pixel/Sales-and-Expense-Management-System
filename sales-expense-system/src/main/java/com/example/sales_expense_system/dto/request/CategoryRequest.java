package com.example.sales_expense_system.dto.request;

import com.example.sales_expense_system.model.Category.CategoryType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    private String categoryName;

    private String description;

    @NotNull(message = "Category type is required")
    private CategoryType type;

    public CategoryRequest() {
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CategoryType getType() {
        return type;
    }

    public void setType(CategoryType type) {
        this.type = type;
    }
}