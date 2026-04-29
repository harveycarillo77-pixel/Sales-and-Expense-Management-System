package com.example.sales_expense_system.dto;

import java.math.BigDecimal;

public class DashboardDto {

    private BigDecimal todaySales;
    private BigDecimal todayExpenses;
    private BigDecimal todayProfit;

    private BigDecimal monthSales;
    private BigDecimal monthExpenses;
    private BigDecimal monthProfit;

    public DashboardDto() {
    }

    public BigDecimal getTodaySales() {
        return todaySales;
    }

    public void setTodaySales(BigDecimal todaySales) {
        this.todaySales = todaySales;
    }

    public BigDecimal getTodayExpenses() {
        return todayExpenses;
    }

    public void setTodayExpenses(BigDecimal todayExpenses) {
        this.todayExpenses = todayExpenses;
    }

    public BigDecimal getTodayProfit() {
        return todayProfit;
    }

    public void setTodayProfit(BigDecimal todayProfit) {
        this.todayProfit = todayProfit;
    }

    public BigDecimal getMonthSales() {
        return monthSales;
    }

    public void setMonthSales(BigDecimal monthSales) {
        this.monthSales = monthSales;
    }

    public BigDecimal getMonthExpenses() {
        return monthExpenses;
    }

    public void setMonthExpenses(BigDecimal monthExpenses) {
        this.monthExpenses = monthExpenses;
    }

    public BigDecimal getMonthProfit() {
        return monthProfit;
    }

    public void setMonthProfit(BigDecimal monthProfit) {
        this.monthProfit = monthProfit;
    }
}