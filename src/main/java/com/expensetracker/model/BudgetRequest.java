package com.expensetracker.model;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class BudgetRequest {
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;
    
    @NotBlank(message = "Period is required")
    private String period;
    
    public BudgetRequest() {
    }
    
    public BudgetRequest(BigDecimal amount, String period) {
        this.amount = amount;
        this.period = period;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getPeriod() {
        return period;
    }
    
    public void setPeriod(String period) {
        this.period = period;
    }
}
