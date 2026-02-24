package com.expensetracker.model;

import java.math.BigDecimal;

public class BudgetStatus {
    
    private String budgetId;
    private BigDecimal amount;
    private BigDecimal spent;
    private BigDecimal remaining;
    private BigDecimal percentageUsed;
    private String period;
    
    public BudgetStatus() {
    }
    
    public BudgetStatus(String budgetId, BigDecimal amount, BigDecimal spent, 
                       BigDecimal remaining, BigDecimal percentageUsed, String period) {
        this.budgetId = budgetId;
        this.amount = amount;
        this.spent = spent;
        this.remaining = remaining;
        this.percentageUsed = percentageUsed;
        this.period = period;
    }
    
    public String getBudgetId() {
        return budgetId;
    }
    
    public void setBudgetId(String budgetId) {
        this.budgetId = budgetId;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public BigDecimal getSpent() {
        return spent;
    }
    
    public void setSpent(BigDecimal spent) {
        this.spent = spent;
    }
    
    public BigDecimal getRemaining() {
        return remaining;
    }
    
    public void setRemaining(BigDecimal remaining) {
        this.remaining = remaining;
    }
    
    public BigDecimal getPercentageUsed() {
        return percentageUsed;
    }
    
    public void setPercentageUsed(BigDecimal percentageUsed) {
        this.percentageUsed = percentageUsed;
    }
    
    public String getPeriod() {
        return period;
    }
    
    public void setPeriod(String period) {
        this.period = period;
    }
}
