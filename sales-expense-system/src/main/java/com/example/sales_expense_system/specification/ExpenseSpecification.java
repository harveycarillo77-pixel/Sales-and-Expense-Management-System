package com.example.sales_expense_system.specification;

import com.example.sales_expense_system.model.Expense;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class ExpenseSpecification {

    public static Specification<Expense> filter(
            String keyword,
            Long categoryId,
            Boolean isVoided,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return (Root<Expense> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {

            Predicate predicate = cb.conjunction();

            // 🔍 Keyword search
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.toLowerCase() + "%";

                Predicate keywordPredicate = cb.or(
                        cb.like(cb.lower(root.get("description")), like),
                        cb.like(cb.lower(root.get("expenseNumber")), like),
                        cb.like(cb.lower(root.get("payeeName")), like),
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
                        cb.greaterThanOrEqualTo(root.get("expenseDate"), startDate));
            }

            if (endDate != null) {
                predicate = cb.and(predicate,
                        cb.lessThanOrEqualTo(root.get("expenseDate"), endDate));
            }

            query.orderBy(
                    cb.desc(root.get("expenseDate")),
                    cb.desc(root.get("expenseId"))
            );

            return predicate;
        };
    }
}