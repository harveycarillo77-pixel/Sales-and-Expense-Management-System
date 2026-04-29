package com.example.sales_expense_system.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.sales_expense_system.model.Expense;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    List<Expense> findAllByOrderByExpenseDateDescExpenseIdDesc();

    List<Expense> findByIsVoidedFalseOrderByExpenseDateDescExpenseIdDesc();

    List<Expense> findByDescriptionContainingIgnoreCaseAndIsVoidedFalseOrderByExpenseDateDescExpenseIdDesc(String description);

    List<Expense> findByExpenseNumberContainingIgnoreCaseAndIsVoidedFalseOrderByExpenseDateDescExpenseIdDesc(String expenseNumber);

    List<Expense> findByPayeeNameContainingIgnoreCaseAndIsVoidedFalseOrderByExpenseDateDescExpenseIdDesc(String payeeName);

    boolean existsByExpenseNumber(String expenseNumber);

    boolean existsByExpenseNumberAndExpenseIdNot(String expenseNumber, Long expenseId);

    @EntityGraph(attributePaths = {
        "category",
        "createdBy",
        "voidedBy",
        "period"
    })
    @Query("""
        SELECT e FROM Expense e
        WHERE (:keyword IS NULL OR
               LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(e.expenseNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(e.payeeName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(e.category.categoryName) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:categoryId IS NULL OR e.category.categoryId = :categoryId)
        AND (:startDate IS NULL OR e.expenseDate >= :startDate)
        AND (:endDate IS NULL OR e.expenseDate <= :endDate)
        AND (:includeVoided = true OR e.isVoided = false)
    """)
    Page<Expense> searchWithFilters(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("includeVoided") boolean includeVoided,
            Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.expenseDate = :date AND e.isVoided = false")
    BigDecimal sumExpensesByDate(LocalDate date);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE MONTH(e.expenseDate) = :month AND YEAR(e.expenseDate) = :year AND e.isVoided = false")
    BigDecimal sumExpensesByMonthAndYear(int month, int year);
}