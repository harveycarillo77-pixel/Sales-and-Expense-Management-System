package com.example.sales_expense_system.service;

import java.util.List;

import com.example.sales_expense_system.dto.request.CategoryRequest;
import com.example.sales_expense_system.model.Category;
import com.example.sales_expense_system.model.Category.CategoryType;

import jakarta.servlet.http.HttpServletRequest;

public interface CategoryService {
    
    List<Category> getAllCategories();

    List<Category> getCategoriesByType(CategoryType type);

    Category getCategoryById(Long id);

    Category createCategory(CategoryRequest request, HttpServletRequest httpRequest);

    Category updateCategory(Long id, CategoryRequest request, HttpServletRequest httpRequest);

    void deleteCategory(Long id, HttpServletRequest httpRequest);
}