package com.example.sales_expense_system.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;

public class ExpenseUpdateRequest {

    private Integer categoryId;
    private String expenseNumber;
    private String description;
    private String payeeName;

    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private LocalDate expenseDate;

    public ExpenseUpdateRequest() {
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getExpenseNumber() {
        return expenseNumber;
    }

    public void setExpenseNumber(String expenseNumber) {
        this.expenseNumber = expenseNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPayeeName() {
        return payeeName;
    }

    public void setPayeeName(String payeeName) {
        this.payeeName = payeeName;
    }


    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }
}