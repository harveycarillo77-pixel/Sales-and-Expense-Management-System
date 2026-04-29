package com.example.sales_expense_system.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ReportDto {

    private String period;
    private String startDate;
    private String endDate;
    private boolean includeVoided;

    private BigDecimal totalSales = BigDecimal.ZERO;
    private BigDecimal totalExpenses = BigDecimal.ZERO;
    private BigDecimal netProfit = BigDecimal.ZERO;

    private long salesCount;
    private long expenseCount;
    private long transactionCount;

    private long completedSalesCount;
    private long completedExpenseCount;
    private long completedTransactionCount;

    private long voidedSalesCount;
    private long voidedExpenseCount;
    private long voidedTransactionCount;

    private List<CategoryTotalDto> salesByCategory = new ArrayList<>();
    private List<CategoryTotalDto> expensesByCategory = new ArrayList<>();
    private List<TransactionRowDto> transactions = new ArrayList<>();

    public ReportDto() {
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public boolean isIncludeVoided() {
        return includeVoided;
    }

    public void setIncludeVoided(boolean includeVoided) {
        this.includeVoided = includeVoided;
    }

    public BigDecimal getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(BigDecimal totalSales) {
        this.totalSales = totalSales;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    public void setTotalExpenses(BigDecimal totalExpenses) {
        this.totalExpenses = totalExpenses;
    }

    public BigDecimal getNetProfit() {
        return netProfit;
    }

    public void setNetProfit(BigDecimal netProfit) {
        this.netProfit = netProfit;
    }

    public long getSalesCount() {
        return salesCount;
    }

    public void setSalesCount(long salesCount) {
        this.salesCount = salesCount;
    }

    public long getExpenseCount() {
        return expenseCount;
    }

    public void setExpenseCount(long expenseCount) {
        this.expenseCount = expenseCount;
    }

    public long getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(long transactionCount) {
        this.transactionCount = transactionCount;
    }

    public long getCompletedSalesCount() {
        return completedSalesCount;
    }

    public void setCompletedSalesCount(long completedSalesCount) {
        this.completedSalesCount = completedSalesCount;
    }

    public long getCompletedExpenseCount() {
        return completedExpenseCount;
    }

    public void setCompletedExpenseCount(long completedExpenseCount) {
        this.completedExpenseCount = completedExpenseCount;
    }

    public long getCompletedTransactionCount() {
        return completedTransactionCount;
    }

    public void setCompletedTransactionCount(long completedTransactionCount) {
        this.completedTransactionCount = completedTransactionCount;
    }

    public long getVoidedSalesCount() {
        return voidedSalesCount;
    }

    public void setVoidedSalesCount(long voidedSalesCount) {
        this.voidedSalesCount = voidedSalesCount;
    }

    public long getVoidedExpenseCount() {
        return voidedExpenseCount;
    }

    public void setVoidedExpenseCount(long voidedExpenseCount) {
        this.voidedExpenseCount = voidedExpenseCount;
    }

    public long getVoidedTransactionCount() {
        return voidedTransactionCount;
    }

    public void setVoidedTransactionCount(long voidedTransactionCount) {
        this.voidedTransactionCount = voidedTransactionCount;
    }

    public List<CategoryTotalDto> getSalesByCategory() {
        return salesByCategory;
    }

    public void setSalesByCategory(List<CategoryTotalDto> salesByCategory) {
        this.salesByCategory = salesByCategory;
    }

    public List<CategoryTotalDto> getExpensesByCategory() {
        return expensesByCategory;
    }

    public void setExpensesByCategory(List<CategoryTotalDto> expensesByCategory) {
        this.expensesByCategory = expensesByCategory;
    }

    public List<TransactionRowDto> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<TransactionRowDto> transactions) {
        this.transactions = transactions;
    }

    public static class CategoryTotalDto {
        private String categoryName;
        private BigDecimal amount = BigDecimal.ZERO;

        public CategoryTotalDto() {
        }

        public CategoryTotalDto(String categoryName, BigDecimal amount) {
            this.categoryName = categoryName;
            this.amount = amount;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }

    public static class TransactionRowDto {
        private String type;
        private String referenceNumber;
        private String date;
        private String name;
        private String categoryName;
        private String status;
        private BigDecimal amount = BigDecimal.ZERO;

        public TransactionRowDto() {
        }

        public TransactionRowDto(
                String type,
                String referenceNumber,
                String date,
                String name,
                String categoryName,
                String status,
                BigDecimal amount
        ) {
            this.type = type;
            this.referenceNumber = referenceNumber;
            this.date = date;
            this.name = name;
            this.categoryName = categoryName;
            this.status = status;
            this.amount = amount;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getReferenceNumber() {
            return referenceNumber;
        }

        public void setReferenceNumber(String referenceNumber) {
            this.referenceNumber = referenceNumber;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }
}