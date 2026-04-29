package com.example.sales_expense_system.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


import com.example.sales_expense_system.model.Sale;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long>, JpaSpecificationExecutor<Sale> {

    Page<Sale> findByIsVoidedFalse(Pageable pageable);

    Page<Sale> findByIsVoided(Boolean isVoided, Pageable pageable);

    List<Sale> findAllByOrderByTransactionDateDescSaleIdDesc();

    List<Sale> findByIsVoidedFalseOrderByTransactionDateDescSaleIdDesc();

    List<Sale> findByItemNameContainingIgnoreCaseAndIsVoidedFalseOrderByTransactionDateDescSaleIdDesc(String itemName);

    List<Sale> findBySaleNumberContainingIgnoreCaseAndIsVoidedFalseOrderByTransactionDateDescSaleIdDesc(String saleNumber);

    List<Sale> findByCustomerNameContainingIgnoreCaseAndIsVoidedFalseOrderByTransactionDateDescSaleIdDesc(String customerName);

    boolean existsBySaleNumber(String saleNumber);

    boolean existsBySaleNumberAndSaleIdNot(String saleNumber, Long saleId);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE s.transactionDate = :date AND s.isVoided = false")
    BigDecimal sumSalesByDate(LocalDate date);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE MONTH(s.transactionDate) = :month AND YEAR(s.transactionDate) = :year AND s.isVoided = false")
    BigDecimal sumSalesByMonthAndYear(int month, int year);

    @EntityGraph(attributePaths = {
        "category",
        "createdBy",
        "voidedBy",
        "period"
    })
    @Query("""
        SELECT s FROM Sale s
        WHERE (:keyword IS NULL OR
               LOWER(s.itemName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(s.saleNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(s.customerName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(s.category.categoryName) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:categoryId IS NULL OR s.category.categoryId = :categoryId)
        AND (:startDate IS NULL OR s.transactionDate >= :startDate)
        AND (:endDate IS NULL OR s.transactionDate <= :endDate)
        AND (:includeVoided = true OR s.isVoided = false)
    """)
    Page<Sale> searchWithFilters(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("includeVoided") boolean includeVoided,
            Pageable pageable
    );
}