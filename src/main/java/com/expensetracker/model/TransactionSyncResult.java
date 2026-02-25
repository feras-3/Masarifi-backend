package com.expensetracker.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionSyncResult {
    
    private boolean success;
    private int newTransactionCount;
    private int updatedTransactionCount;
    private List<String> errors;
    private LocalDateTime syncedAt;
    
    public TransactionSyncResult() {
        this.errors = new ArrayList<>();
        this.syncedAt = LocalDateTime.now();
    }
    
    public TransactionSyncResult(boolean success, int newTransactionCount, int updatedTransactionCount) {
        this();
        this.success = success;
        this.newTransactionCount = newTransactionCount;
        this.updatedTransactionCount = updatedTransactionCount;
    }
    
    // Getters and Setters
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public int getNewTransactionCount() {
        return newTransactionCount;
    }
    
    public void setNewTransactionCount(int newTransactionCount) {
        this.newTransactionCount = newTransactionCount;
    }
    
    public int getUpdatedTransactionCount() {
        return updatedTransactionCount;
    }
    
    public void setUpdatedTransactionCount(int updatedTransactionCount) {
        this.updatedTransactionCount = updatedTransactionCount;
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
    
    public void addError(String error) {
        this.errors.add(error);
    }
    
    public LocalDateTime getSyncedAt() {
        return syncedAt;
    }
    
    public void setSyncedAt(LocalDateTime syncedAt) {
        this.syncedAt = syncedAt;
    }
}
