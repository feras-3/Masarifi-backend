package com.expensetracker.service;

import com.expensetracker.model.PlaidAccount;
import com.expensetracker.model.TransactionSyncResult;
import com.expensetracker.repository.PlaidAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Scheduled job to sync transactions from Plaid daily
 * Requirements: 14.6
 */
@Service
public class TransactionSyncScheduler {
    
    @Autowired
    private PlaidService plaidService;
    
    @Autowired
    private PlaidAccountRepository plaidAccountRepository;
    
    /**
     * Run transaction sync daily at 2 AM
     * Cron expression: second, minute, hour, day of month, month, day of week
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void syncAllAccounts() {
        System.out.println("Starting scheduled transaction sync...");
        
        // Get all active Plaid accounts
        List<PlaidAccount> accounts = plaidAccountRepository.findAll().stream()
            .filter(PlaidAccount::getIsActive)
            .toList();
        
        for (PlaidAccount account : accounts) {
            try {
                String userId = account.getUserId();
                TransactionSyncResult result = plaidService.syncTransactions(userId);
                
                if (result.isSuccess()) {
                    System.out.println("Successfully synced " + result.getNewTransactionCount() + 
                                     " transactions for user " + userId);
                } else {
                    System.err.println("Failed to sync transactions for user " + userId + 
                                     ": " + String.join(", ", result.getErrors()));
                }
            } catch (Exception e) {
                System.err.println("Error syncing transactions for account " + account.getId() + 
                                 ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("Scheduled transaction sync completed.");
    }
}
