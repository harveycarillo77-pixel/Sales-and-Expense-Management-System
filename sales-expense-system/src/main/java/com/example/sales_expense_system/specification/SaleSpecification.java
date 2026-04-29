package com.example.sales_expense_system.specification;

import com.example.sales_expense_system.model.Sale;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class SaleSpecification {

    public static Specification<Sale> filter(
            String keyword,
            Long categoryId,
            Boolean isVoided,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return (Root<Sale> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {

            Predicate predicate = cb.conjunction();

            // 🔍 Keyword search
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.toLowerCase() + "%";

                Predicate keywordPredicate = cb.or(
                        cb.like(cb.lower(root.get("itemName")), like),
                        cb.like(cb.lower(root.get("saleNumber")), like),
                        cb.like(cb.lower(root.get("customerName")), like),
                        cb.like(cb.lower(root.get("category").get("categoryName")), like)
                );

                predicate = cb.and(predicate, keywordPredicate);
            }

            // 📂 Category filter
            if (categoryId != null) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("category").get("categoryId"), categoryId));
            }

            // 🚫 Voided filter
            if (isVoided != null) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("isVoided"), isVoided));
            }

            // 📅 Date range
            if (startDate != null) {
                predicate = cb.and(predicate,
                        cb.greaterThanOrEqualTo(root.get("transactionDate"), startDate));
            }

            if (endDate != null) {
                predicate = cb.and(predicate,
                        cb.lessThanOrEqualTo(root.get("transactionDate"), endDate));
            }

            query.orderBy(
                    cb.desc(root.get("transactionDate")),
                    cb.desc(root.get("saleId"))
            );

            return predicate;
        };
    }
}