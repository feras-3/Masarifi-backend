package com.expensetracker.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "alerts", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_type_period", columnNames = {"user_id", "type", "period"})
})
public class Alert {
    
    @Id
    @Column(name = "id", length = 36)
    private String id;
    
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;
    
    @Column(name = "type", nullable = false, length = 20)
    @NotBlank(message = "Type is required")
    private String type; // WARNING or CRITICAL
    
    @Column(name = "budget_amount", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Budget amount is required")
    private BigDecimal budgetAmount;
    
    @Column(name = "current_spending", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Current spending is required")
    private BigDecimal currentSpending;
    
    @Column(name = "percentage_exceeded", nullable = false, precision = 5, scale = 2)
    @NotNull(message = "Percentage exceeded is required")
    private BigDecimal percentageExceeded;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "dismissed", nullable = false)
    private boolean dismissed;
    
    @Column(name = "period", nullable = false, length = 20)
    @NotBlank(message = "Period is required")
    private String period;
    
    public Alert() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.dismissed = false;
    }
    
    public Alert(String userId, String type, BigDecimal budgetAmount, BigDecimal currentSpending, 
                 BigDecimal percentageExceeded, String period) {
        this();
        this.userId = userId;
        this.type = type;
        this.budgetAmount = budgetAmount;
        this.currentSpending = currentSpending;
        this.percentageExceeded = percentageExceeded;
        this.period = period;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public BigDecimal getBudgetAmount() {
        return budgetAmount;
    }
    
    public void setBudgetAmount(BigDecimal budgetAmount) {
        this.budgetAmount = budgetAmount;
    }
    
    public BigDecimal getCurrentSpending() {
        return currentSpending;
    }
    
    public void setCurrentSpending(BigDecimal currentSpending) {
        this.currentSpending = currentSpending;
    }
    
    public BigDecimal getPercentageExceeded() {
        return percentageExceeded;
    }
    
    public void setPercentageExceeded(BigDecimal percentageExceeded) {
        this.percentageExceeded = percentageExceeded;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public boolean isDismissed() {
        return dismissed;
    }
    
    public void setDismissed(boolean dismissed) {
        this.dismissed = dismissed;
    }
    
    public String getPeriod() {
        return period;
    }
    
    public void setPeriod(String period) {
        this.period = period;
    }
}
