package com.expensetracker.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_user_date", columnList = "user_id, date"),
    @Index(name = "idx_user_category", columnList = "user_id, category"),
    @Index(name = "idx_plaid_transaction", columnList = "plaid_transaction_id"),
    @Index(name = "idx_source", columnList = "source")
})
public class Transaction {
    
    @Id
    @Column(name = "id", length = 36)
    private String id;
    
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;
    
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @NotNull(message = "Amount is required")
    private BigDecimal amount;
    
    @Column(name = "date", nullable = false)
    @NotNull(message = "Date is required")
    @PastOrPresent(message = "Date cannot be in the future")
    private LocalDate date;
    
    @Column(name = "description", nullable = false, length = 200)
    @NotBlank(message = "Description is required")
    @Size(max = 200, message = "Description cannot exceed 200 characters")
    private String description;
    
    @Column(name = "category", nullable = false, length = 50)
    @NotBlank(message = "Category is required")
    private String category;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "plaid_transaction_id", unique = true, length = 100)
    private String plaidTransactionId;
    
    @Column(name = "merchant_name", length = 200)
    private String merchantName;
    
    @Column(name = "plaid_category", length = 100)
    private String plaidCategory;
    
    @Column(name = "source", nullable = false, length = 20)
    private String source = "MANUAL";
    
    public Transaction() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }
    
    public Transaction(String userId, BigDecimal amount, LocalDate date, String description, String category) {
        this();
        this.userId = userId;
        this.amount = amount;
        this.date = date;
        this.description = description;
        this.category = category;
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
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public LocalDate getDate() {
        return date;
    }
    
    public void setDate(LocalDate date) {
        this.date = date;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getPlaidTransactionId() {
        return plaidTransactionId;
    }
    
    public void setPlaidTransactionId(String plaidTransactionId) {
        this.plaidTransactionId = plaidTransactionId;
    }
    
    public String getMerchantName() {
        return merchantName;
    }
    
    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }
    
    public String getPlaidCategory() {
        return plaidCategory;
    }
    
    public void setPlaidCategory(String plaidCategory) {
        this.plaidCategory = plaidCategory;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
}
