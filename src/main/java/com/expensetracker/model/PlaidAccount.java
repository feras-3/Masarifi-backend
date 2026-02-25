package com.expensetracker.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "plaid_accounts", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "item_id"}),
       indexes = @Index(name = "idx_user_active", columnList = "user_id, is_active"))
public class PlaidAccount {
    
    @Id
    private String id;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
    private String accessToken; // Encrypted
    
    @Column(name = "item_id", nullable = false, length = 100)
    private String itemId;
    
    @Column(name = "institution_name", nullable = false, length = 200)
    private String institutionName;
    
    @Column(name = "account_name", length = 200)
    private String accountName;
    
    @Column(name = "account_mask", length = 4)
    private String accountMask;
    
    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt;
    
    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    public PlaidAccount() {
        this.id = UUID.randomUUID().toString();
        this.linkedAt = LocalDateTime.now();
    }
    
    public PlaidAccount(String userId, String accessToken, String itemId, String institutionName) {
        this();
        this.userId = userId;
        this.accessToken = accessToken;
        this.itemId = itemId;
        this.institutionName = institutionName;
    }
    
    // Getters and Setters
    
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
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getItemId() {
        return itemId;
    }
    
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
    
    public String getInstitutionName() {
        return institutionName;
    }
    
    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }
    
    public String getAccountName() {
        return accountName;
    }
    
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }
    
    public String getAccountMask() {
        return accountMask;
    }
    
    public void setAccountMask(String accountMask) {
        this.accountMask = accountMask;
    }
    
    public LocalDateTime getLinkedAt() {
        return linkedAt;
    }
    
    public void setLinkedAt(LocalDateTime linkedAt) {
        this.linkedAt = linkedAt;
    }
    
    public LocalDateTime getLastSyncAt() {
        return lastSyncAt;
    }
    
    public void setLastSyncAt(LocalDateTime lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
