package com.example.sales_expense_system.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AccountingPeriodDto {

    private Long periodId;
    private String periodName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isLocked;
    private String lockedByUsername;
    private LocalDateTime lockedAt;

    public AccountingPeriodDto() {
    }

    public Long getPeriodId() {
        return periodId;
    }

    public void setPeriodId(Long periodId) {
        this.periodId = periodId;
    }

    public String getPeriodName() {
        return periodName;
    }

    public void setPeriodName(String periodName) {
        this.periodName = periodName;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Boolean getIsLocked() {
        return isLocked;
    }

    public void setIsLocked(Boolean isLocked) {
        this.isLocked = isLocked;
    }

    public String getLockedByUsername() {
        return lockedByUsername;
    }

    public void setLockedByUsername(String lockedByUsername) {
        this.lockedByUsername = lockedByUsername;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(LocalDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }
}