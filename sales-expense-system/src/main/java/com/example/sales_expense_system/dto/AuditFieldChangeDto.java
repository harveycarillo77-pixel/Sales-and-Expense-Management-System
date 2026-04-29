package com.example.sales_expense_system.dto;

public class AuditFieldChangeDto {

    private String field;
    private String oldValue;
    private String newValue;

    public AuditFieldChangeDto(String field, String oldValue, String newValue) {
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getField() { return field; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
}