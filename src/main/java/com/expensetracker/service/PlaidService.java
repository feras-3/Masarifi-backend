package com.expensetracker.service;

import com.expensetracker.model.PlaidAccount;
import com.expensetracker.model.Transaction;
import com.expensetracker.model.TransactionSyncResult;
import com.expensetracker.repository.PlaidAccountRepository;
import com.expensetracker.repository.TransactionRepository;
import com.plaid.client.model.*;
import com.plaid.client.request.PlaidApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrofit2.Response;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Service for integrating with Plaid API
 * Requirements: 13.1, 13.2, 13.5, 13.6, 13.7, 14.1, 14.2, 14.3, 14.4, 14.5, 17.1, 17.2, 17.5, 17.6
 */
@Service
public class PlaidService {
    
    @Autowired
    private PlaidApi plaidClient;
    
    @Autowired
    private PlaidAccountRepository plaidAccountRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private EncryptionService encryptionService;
    
    @Autowired
    private CategoryMapper categoryMapper;
    
    @Autowired
    private BudgetService budgetService;
    
    @Value("${plaid.client.id}")
    private String clientId;
    
    /**
     * Create a link token for Plaid Link initialization
     * Requirements: 13.1, 13.2
     */
    public String createLinkToken(String userId) {
        try {
            LinkTokenCreateRequest request = new LinkTokenCreateRequest()
                .user(new LinkTokenCreateRequestUser().clientUserId(userId))
                .clientName("Expense Tracker")
                .products(Arrays.asList(Products.TRANSACTIONS))
                .countryCodes(Arrays.asList(CountryCode.US))
                .language("en");
            
            Response<LinkTokenCreateResponse> response = plaidClient
                .linkTokenCreate(request)
                .execute();
            
            if (response.isSuccessful() && response.body() != null) {
                return response.body().getLinkToken();
            } else {
                throw new RuntimeException("Failed to create link token: " + response.message());
            }
        } catch (IOException e) {
            throw new RuntimeException("Error creating link token", e);
        }
    }
    
    /**
     * Exchange public token for access token and store it
     * Requirements: 13.5, 13.6, 13.7, 17.1
     */
    @Transactional
    public PlaidAccount exchangePublicToken(String publicToken, String userId) {
        try {
            // Exchange public token for access token
            ItemPublicTokenExchangeRequest request = new ItemPublicTokenExchangeRequest()
                .publicToken(publicToken);
            
            Response<ItemPublicTokenExchangeResponse> response = plaidClient
                .itemPublicTokenExchange(request)
                .execute();
            
            if (!response.isSuccessful() || response.body() == null) {
                throw new RuntimeException("Failed to exchange public token: " + response.message());
            }
            
            String accessToken = response.body().getAccessToken();
            String itemId = response.body().getItemId();
            
            // Get institution information
            ItemGetRequest itemRequest = new ItemGetRequest()
                .accessToken(accessToken);
            
            Response<ItemGetResponse> itemResponse = plaidClient
                .itemGet(itemRequest)
                .execute();
            
            String institutionId = null;
            if (itemResponse.isSuccessful() && itemResponse.body() != null) {
                institutionId = itemResponse.body().getItem().getInstitutionId();
            }
            
            String institutionName = "Unknown Bank";
            if (institutionId != null) {
                InstitutionsGetByIdRequest instRequest = new InstitutionsGetByIdRequest()
                    .institutionId(institutionId)
                    .countryCodes(Arrays.asList(CountryCode.US));
                
                Response<InstitutionsGetByIdResponse> instResponse = plaidClient
                    .institutionsGetById(instRequest)
                    .execute();
                
                if (instResponse.isSuccessful() && instResponse.body() != null) {
                    institutionName = instResponse.body().getInstitution().getName();
                }
            }
            
            // Encrypt access token before storing
            String encryptedToken = encryptionService.encrypt(accessToken);
            
            // Create and save PlaidAccount
            PlaidAccount plaidAccount = new PlaidAccount(userId, encryptedToken, itemId, institutionName);
            
            return plaidAccountRepository.save(plaidAccount);
            
        } catch (IOException e) {
            throw new RuntimeException("Error exchanging public token", e);
        }
    }
    
    /**
     * Sync transactions from Plaid for a user
     * Requirements: 14.1, 14.2, 14.3, 14.4, 14.5
     */
    @Transactional
    public TransactionSyncResult syncTransactions(String userId) {
        TransactionSyncResult result = new TransactionSyncResult();
        int newCount = 0;
        
        try {
            List<PlaidAccount> accounts = plaidAccountRepository.findByUserIdAndIsActive(userId, true);
            
            if (accounts.isEmpty()) {
                result.setSuccess(false);
                result.addError("No active Plaid accounts found");
                return result;
            }
            
            for (PlaidAccount account : accounts) {
                try {
                    newCount += syncAccountTransactions(account, userId);
                    account.setLastSyncAt(LocalDateTime.now());
                    plaidAccountRepository.save(account);
                } catch (Exception e) {
                    result.addError("Failed to sync account " + account.getInstitutionName() + ": " + e.getMessage());
                }
            }
            
            result.setSuccess(true);
            result.setNewTransactionCount(newCount);
            
            // Trigger budget recalculation if transactions were added
            if (newCount > 0) {
                try {
                    budgetService.recalculateBalance(userId);
                } catch (NoSuchElementException e) {
                    // No budget exists yet, that's okay
                }
            }
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.addError("Error syncing transactions: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Sync transactions for a specific Plaid account
     */
    private int syncAccountTransactions(PlaidAccount account, String userId) throws IOException {
        // Decrypt access token
        String accessToken = encryptionService.decrypt(account.getAccessToken());
        
        // Get transactions from the past 30 days
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        
        TransactionsGetRequest request = new TransactionsGetRequest()
            .accessToken(accessToken)
            .startDate(startDate)
            .endDate(endDate);
        
        Response<TransactionsGetResponse> response = plaidClient
            .transactionsGet(request)
            .execute();
        
        if (!response.isSuccessful() || response.body() == null) {
            throw new RuntimeException("Failed to fetch transactions: " + response.message());
        }
        
        List<com.plaid.client.model.Transaction> plaidTransactions = response.body().getTransactions();
        int newCount = 0;
        
        for (com.plaid.client.model.Transaction plaidTx : plaidTransactions) {
            // Skip pending transactions
            if (plaidTx.getPending() != null && plaidTx.getPending()) {
                continue;
            }
            
            // Check if transaction already exists
            if (transactionRepository.findByPlaidTransactionId(plaidTx.getTransactionId()).isPresent()) {
                continue;
            }
            
            // Map Plaid category to application category
            String plaidCategory = plaidTx.getPersonalFinanceCategory() != null 
                ? plaidTx.getPersonalFinanceCategory().getPrimary() 
                : "Other";
            String appCategory = categoryMapper.mapPlaidCategory(plaidCategory);
            
            // Create transaction
            Transaction transaction = new Transaction();
            transaction.setUserId(userId);
            transaction.setAmount(BigDecimal.valueOf(Math.abs(plaidTx.getAmount())));
            transaction.setDate(plaidTx.getDate());
            transaction.setDescription(plaidTx.getName() != null ? plaidTx.getName() : "Plaid Transaction");
            transaction.setCategory(appCategory);
            transaction.setPlaidTransactionId(plaidTx.getTransactionId());
            transaction.setMerchantName(plaidTx.getMerchantName());
            transaction.setPlaidCategory(plaidCategory);
            transaction.setSource("PLAID");
            
            transactionRepository.save(transaction);
            newCount++;
        }
        
        return newCount;
    }
    
    /**
     * Get linked accounts for a user
     */
    public List<PlaidAccount> getLinkedAccounts(String userId) {
        return plaidAccountRepository.findByUserId(userId);
    }
    
    /**
     * Unlink a Plaid account
     * Requirements: 17.5, 17.6
     */
    @Transactional
    public void unlinkAccount(String userId) {
        List<PlaidAccount> accounts = plaidAccountRepository.findByUserIdAndIsActive(userId, true);
        
        for (PlaidAccount account : accounts) {
            account.setIsActive(false);
            plaidAccountRepository.save(account);
        }
    }
    
    /**
     * Check if access token is valid
     * Requirements: 17.3, 17.4
     */
    public boolean isAccessTokenValid(String userId) {
        Optional<PlaidAccount> accountOpt = plaidAccountRepository
            .findFirstByUserIdAndIsActiveOrderByLinkedAtDesc(userId, true);
        
        if (accountOpt.isEmpty()) {
            return false;
        }
        
        try {
            PlaidAccount account = accountOpt.get();
            String accessToken = encryptionService.decrypt(account.getAccessToken());
            
            // Try to get item to verify token is valid
            ItemGetRequest request = new ItemGetRequest()
                .accessToken(accessToken);
            
            Response<ItemGetResponse> response = plaidClient
                .itemGet(request)
                .execute();
            
            return response.isSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Handle webhook notifications from Plaid
     * Requirements: 18.1, 18.2, 18.3, 18.4, 18.5
     */
    public void handleWebhook(com.expensetracker.model.WebhookRequest webhookRequest) {
        String webhookType = webhookRequest.getWebhookType();
        String webhookCode = webhookRequest.getWebhookCode();
        String itemId = webhookRequest.getItemId();
        
        System.out.println("Received webhook: type=" + webhookType + ", code=" + webhookCode + ", itemId=" + itemId);
        
        // Handle TRANSACTIONS webhooks
        if ("TRANSACTIONS".equals(webhookType)) {
            if ("DEFAULT_UPDATE".equals(webhookCode) || "INITIAL_UPDATE".equals(webhookCode)) {
                // Find the account by item ID
                Optional<PlaidAccount> accountOpt = plaidAccountRepository.findAll().stream()
                    .filter(acc -> acc.getItemId().equals(itemId) && acc.getIsActive())
                    .findFirst();
                
                if (accountOpt.isPresent()) {
                    PlaidAccount account = accountOpt.get();
                    String userId = account.getUserId();
                    
                    // Trigger transaction sync asynchronously
                    try {
                        TransactionSyncResult result = syncTransactions(userId);
                        System.out.println("Webhook triggered sync completed: " + 
                                         result.getNewTransactionCount() + " new transactions");
                    } catch (Exception e) {
                        System.err.println("Error syncing transactions from webhook: " + e.getMessage());
                    }
                } else {
                    System.err.println("No active account found for item ID: " + itemId);
                }
            }
        }
        
        // Handle error webhooks
        if (webhookRequest.getError() != null) {
            System.err.println("Webhook error: " + webhookRequest.getError().getErrorCode() + 
                             " - " + webhookRequest.getError().getErrorMessage());
        }
    }
}
