package com.example.sales_expense_system.util;

public final class AuditActions {

    private AuditActions() {}

    public static final String LOGIN = "LOGIN";
    public static final String LOGOUT = "LOGOUT";

    public static final String CREATE_SALE = "CREATE_SALE";
    public static final String UPDATE_SALE = "UPDATE_SALE";
    public static final String VOID_SALE = "VOID_SALE";
    public static final String PERMANENT_DELETE_SALE = "PERMANENT_DELETE_SALE";

    public static final String CREATE_EXPENSE = "CREATE_EXPENSE";
    public static final String UPDATE_EXPENSE = "UPDATE_EXPENSE";
    public static final String VOID_EXPENSE = "VOID_EXPENSE";
    public static final String PERMANENT_DELETE_EXPENSE = "PERMANENT_DELETE_EXPENSE";

    public static final String ACCOUNT_CHANGE = "ACCOUNT_CHANGE";
} 

