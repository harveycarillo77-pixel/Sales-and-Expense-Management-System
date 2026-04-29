package com.example.sales_expense_system.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.sales_expense_system.dto.CategoryDto;
import com.example.sales_expense_system.dto.request.CategoryRequest;
import com.example.sales_expense_system.model.Category;
import com.example.sales_expense_system.model.Category.CategoryType;
import com.example.sales_expense_system.service.CategoryService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<List<CategoryDto>> getAllCategories(
            @RequestParam(required = false) CategoryType type) {

        List<Category> categories = (type == null)
                ? categoryService.getAllCategories()
                : categoryService.getCategoriesByType(type);

        List<CategoryDto> response = categories.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<CategoryDto> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(mapToDto(categoryService.getCategoryById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody CategoryRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(mapToDto(categoryService.createCategory(request, httpRequest)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryDto> updateCategory(@PathVariable Long id,
                                                      @Valid @RequestBody CategoryRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(mapToDto(categoryService.updateCategory(id, request, httpRequest)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id, HttpServletRequest httpRequest) {
        categoryService.deleteCategory(id, httpRequest);
        return ResponseEntity.noContent().build();
    }

    private CategoryDto mapToDto(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.setCategoryId(category.getCategoryId());
        dto.setCategoryName(category.getCategoryName());
        dto.setDescription(category.getDescription());
        dto.setType(category.getType());
        return dto;
    }
}