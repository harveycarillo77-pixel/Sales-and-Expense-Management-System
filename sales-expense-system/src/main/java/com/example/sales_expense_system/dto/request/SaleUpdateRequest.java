package com.example.sales_expense_system.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;

public class SaleUpdateRequest {

    private Integer categoryId;

    private String orNumber;

    private String itemName;

    private String customerName;

    @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    @DecimalMin(value = "0.01", message = "Unit price must be greater than 0")
    private BigDecimal unitPrice;

    private LocalDate transactionDate;

    public SaleUpdateRequest() {
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getOrNumber() {
        return orNumber;
    }

    public void setOrNumber(String orNumber) {
        this.orNumber = orNumber;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }
}