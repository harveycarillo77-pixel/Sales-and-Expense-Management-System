package com.example.sales_expense_system.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.sales_expense_system.model.AccountingPeriod;

@Repository
public interface AccountingPeriodRepository extends JpaRepository<AccountingPeriod, Long> {

    Optional<AccountingPeriod> findByIsLocked(Boolean isLocked);

    Optional<AccountingPeriod> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(
            LocalDate date1, LocalDate date2
    );

    List<AccountingPeriod> findAllByStartDateLessThanEqualAndEndDateGreaterThanEqual(
            LocalDate endDate, LocalDate startDate
    );

        boolean existsByIsLockedTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            LocalDate date1,
            LocalDate date2
    );
}