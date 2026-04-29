package com.example.sales_expense_system.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.sales_expense_system.dto.request.CategoryRequest;
import com.example.sales_expense_system.model.Category;
import com.example.sales_expense_system.model.Category.CategoryType;
import com.example.sales_expense_system.model.User;
import com.example.sales_expense_system.repository.CategoryRepository;
import com.example.sales_expense_system.repository.UserRepository;
import com.example.sales_expense_system.service.AuditLogService;
import com.example.sales_expense_system.service.CategoryService;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    public CategoryServiceImpl(
            CategoryRepository categoryRepository,
            AuditLogService auditLogService,
            UserRepository userRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getCategoriesByType(CategoryType type) {
        return categoryRepository.findByType(type);
    }

    @Override
    @Transactional(readOnly = true)
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
    }

    @Override
    @Transactional
    public Category createCategory(CategoryRequest request, HttpServletRequest httpRequest) {
        String categoryName = request.getCategoryName().trim();

        if (categoryRepository.existsByCategoryName(categoryName)) {
            throw new RuntimeException("Category name already exists");
        }

        Category category = new Category();
        category.setCategoryName(categoryName);
        category.setDescription(
                request.getDescription() == null || request.getDescription().trim().isEmpty()
                        ? null
                        : request.getDescription().trim()
        );
        category.setType(request.getType());

        Category savedCategory = categoryRepository.save(category);

        auditLogService.log(
                "CREATE CATEGORY",
                "Categories",
                "categories",
                savedCategory.getCategoryId(),
                savedCategory.getCategoryName(),
                "Created category " + savedCategory.getCategoryName() + ".",
                "New category was added.",
                null,
                categorySnapshot(savedCategory),
                """
                [
                  {"field":"categoryName","oldValue":"","newValue":"%s"},
                  {"field":"description","oldValue":"","newValue":"%s"},
                  {"field":"type","oldValue":"","newValue":"%s"}
                ]
                """.formatted(
                        escapeJson(savedCategory.getCategoryName()),
                        escapeJson(savedCategory.getDescription()),
                        escapeJson(savedCategory.getType() != null ? savedCategory.getType().name() : "")
                ).replace("\n", "").replace("  ", ""),
                getCurrentUser(),
                httpRequest
        );

        return savedCategory;
    }

    @Override
    @Transactional
    public Category updateCategory(Long id, CategoryRequest request, HttpServletRequest httpRequest) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        String oldCategoryName = existing.getCategoryName();
        String oldDescription = existing.getDescription();
        String oldType = existing.getType() != null ? existing.getType().name() : "";
        String oldValues = categorySnapshot(existing);

        String categoryName = request.getCategoryName().trim();

        if (!categoryName.equals(existing.getCategoryName())
                && categoryRepository.existsByCategoryName(categoryName)) {
            throw new RuntimeException("Category name already exists");
        }

        existing.setCategoryName(categoryName);
        existing.setDescription(
                request.getDescription() == null || request.getDescription().trim().isEmpty()
                        ? null
                        : request.getDescription().trim()
        );
        existing.setType(request.getType());

        List<String> changes = new ArrayList<>();
        addChangedField(changes, "categoryName", oldCategoryName, existing.getCategoryName());
        addChangedField(changes, "description", oldDescription, existing.getDescription());
        addChangedField(changes, "type", oldType, existing.getType() != null ? existing.getType().name() : "");

        Category savedCategory = categoryRepository.save(existing);

        auditLogService.log(
                "UPDATE CATEGORY",
                "Categories",
                "categories",
                savedCategory.getCategoryId(),
                savedCategory.getCategoryName(),
                "Updated category " + savedCategory.getCategoryName() + ".",
                changes.isEmpty() ? "Category record was saved with no detected field changes." : "Category details were updated.",
                oldValues,
                categorySnapshot(savedCategory),
                buildChangedFieldsJson(changes),
                getCurrentUser(),
                httpRequest
        );

        return savedCategory;
    }

    @Override
    @Transactional
    public void deleteCategory(Long id, HttpServletRequest httpRequest) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        String oldValues = categorySnapshot(existing);
        String targetDisplay = existing.getCategoryName();

        categoryRepository.delete(existing);

        auditLogService.log(
                "DELETE CATEGORY",
                "Categories",
                "categories",
                id,
                targetDisplay,
                "Deleted category " + targetDisplay + ".",
                "Category record was deleted.",
                oldValues,
                null,
                """
                [
                  {"field":"deleted","oldValue":"false","newValue":"true"}
                ]
                """.replace("\n", "").replace("  ", ""),
                getCurrentUser(),
                httpRequest
        );
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String categorySnapshot(Category category) {
        return """
            {
              "categoryName":"%s",
              "description":"%s",
              "type":"%s"
            }
            """.formatted(
                escapeJson(category.getCategoryName()),
                escapeJson(category.getDescription()),
                escapeJson(category.getType() != null ? category.getType().name() : "")
            ).replace("\n", "").replace("  ", "");
    }

    private void addChangedField(List<String> changes, String field, Object oldValue, Object newValue) {
        String oldText = oldValue == null ? "" : String.valueOf(oldValue);
        String newText = newValue == null ? "" : String.valueOf(newValue);

        if (!oldText.equals(newText)) {
            changes.add("""
                {"field":"%s","oldValue":"%s","newValue":"%s"}
                """.formatted(
                    escapeJson(field),
                    escapeJson(oldText),
                    escapeJson(newText)
                ).replace("\n", "").replace("  ", ""));
        }
    }

    private String buildChangedFieldsJson(List<String> changes) {
        return "[" + String.join(",", changes) + "]";
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepository.findWithRoleByUsername(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}