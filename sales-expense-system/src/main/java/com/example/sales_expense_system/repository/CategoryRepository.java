package com.example.sales_expense_system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.sales_expense_system.model.Category;
import com.example.sales_expense_system.model.Category.CategoryType;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByCategoryName(String categoryName);

    boolean existsByCategoryName(String categoryName);

    List<Category> findByType(CategoryType type);
}