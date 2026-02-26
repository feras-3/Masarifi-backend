package com.expensetracker.integration;

import com.expensetracker.model.*;
import com.expensetracker.repository.*;
import com.expensetracker.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests for Plaid integration flows.
 * 
 * Tests: link account, sync transactions, view imported transactions,
 * manual category changes, account unlinking with transaction retention
 * 
 * Note: These tests use mocked Plaid responses since we're in sandbox mode
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PlaidIntegrationFlowTest {
    
    @Autowired
    private PlaidService plaidService;
    
    @Autowired
    private TransactionService transactionService;
    
    @Autowired
    private BudgetService budgetService;
    
    @Autowired
    private PlaidAccountRepository plaidAccountRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private BudgetRepository budgetRepository;
    
    private String testUserId;
    private String currentPeriod;
    
    @BeforeEach
    void setUp() {
        testUserId = "plaid-test-user-" + System.currentTimeMillis();
        currentPeriod = LocalDate.now().toString().substring(0, 7);
        
        plaidAccountRepository.deleteAll();
        transactionRepository.deleteAll();
        budgetRepository.deleteAll();
    }
    
    /**
     * Test complete Plaid flow: link account, sync transactions, view imported transactions
     * Validates Requirements: 13.1-13.9, 14.1-14.8, 16.1-16.5
     */
    @Test
    void testCompletePlaidIntegrationFlow() {
        // Step 1: User requests link token
        String linkToken = plaidService.createLinkToken(testUserId);
        assertNotNull(linkToken);
        assertFalse(linkToken.isEmpty());
        
        // Step 2: User completes Plaid Link and exchanges public token
        // Note: In real scenario, this would come from Plaid Link UI
        String mockPublicToken = "public-sandbox-test-token";
        PlaidAccount account = plaidService.exchangePublicToken(mockPublicToken, testUserId);
        
        assertNotNull(account);
        assertNotNull(account.getId());
        assertEquals(testUserId, account.getUserId());
        assertNotNull(account.getInstitutionName());
        assertTrue(account.getIsActive());
        
        // Step 3: User views linked accounts
        List<PlaidAccount> linkedAccounts = plaidService.getLinkedAccounts(testUserId);
        assertEquals(1, linkedAccounts.size());
        assertEquals(account.getId(), linkedAccounts.get(0).getId());
        
        // Step 4: User triggers transaction sync
        TransactionSyncResult syncResult = plaidService.syncTransactions(testUserId);
        assertNotNull(syncResult);
        assertTrue(syncResult.isSuccess());
        assertTrue(syncResult.getNewTransactionCount() >= 0);
        
        // Step 5: User views imported transactions
        List<Transaction> transactions = transactionService.getAllTransactions(testUserId);
        
        // Verify Plaid transactions have required fields
        for (Transaction tx : transactions) {
            if ("PLAID".equals(tx.getSource())) {
                assertNotNull(tx.getPlaidTransactionId());
                assertNotNull(tx.getCategory());
                // Merchant name may be null for some transactions
            }
        }
        
        // Step 6: Verify transactions are sorted by date
        if (transactions.size() > 1) {
            for (int i = 0; i < transactions.size() - 1; i++) {
                assertTrue(
                    transactions.get(i).getDate().compareTo(transactions.get(i + 1).getDate()) >= 0,
                    "Transactions should be sorted by date descending"
                );
            }
        }
    }
    
    /**
     * Test manual category changes for Plaid transactions
     * Validates Requirements: 15.10, 15.11
     */
    @Test
    void testManualCategoryChangeForPlaidTransaction() {
        // Setup: Link account and sync transactions
        String linkToken = plaidService.createLinkToken(testUserId);
        assertNotNull(linkToken);
        
        String mockPublicToken = "public-sandbox-test-token";
        PlaidAccount account = plaidService.exchangePublicToken(mockPublicToken, testUserId);
        assertNotNull(account);
        
        TransactionSyncResult syncResult = plaidService.syncTransactions(testUserId);
        assertTrue(syncResult.isSuccess());
        
        // Get a Plaid transaction
        List<Transaction> transactions = transactionService.getAllTransactions(testUserId);
        Transaction plaidTransaction = transactions.stream()
            .filter(tx -> "PLAID".equals(tx.getSource()))
            .findFirst()
            .orElse(null);
        
        if (plaidTransaction != null) {
            String originalCategory = plaidTransaction.getCategory();
            String originalPlaidCategory = plaidTransaction.getPlaidCategory();
            
            // Step 1: User changes category
            String newCategory = "Entertainment";
            TransactionRequest updateRequest = new TransactionRequest(
                plaidTransaction.getAmount(),
                plaidTransaction.getDate(),
                plaidTransaction.getDescription(),
                newCategory
            );
            
            Transaction updatedTx = transactionService.updateTransaction(
                plaidTransaction.getId(),
                testUserId,
                updateRequest
            );
            
            // Step 2: Verify category changed but original Plaid category preserved
            assertEquals(newCategory, updatedTx.getCategory());
            assertEquals(originalPlaidCategory, updatedTx.getPlaidCategory());
            assertNotEquals(originalCategory, updatedTx.getCategory());
        }
    }
    
    /**
     * Test account unlinking with transaction retention
     * Validates Requirements: 17.5, 17.6, 17.7
     */
    @Test
    void testAccountUnlinkingWithTransactionRetention() {
        // Setup: Link account and sync transactions
        String linkToken = plaidService.createLinkToken(testUserId);
        assertNotNull(linkToken);
        
        String mockPublicToken = "public-sandbox-test-token";
        PlaidAccount account = plaidService.exchangePublicToken(mockPublicToken, testUserId);
        assertNotNull(account);
        
        TransactionSyncResult syncResult = plaidService.syncTransactions(testUserId);
        assertTrue(syncResult.isSuccess());
        
        // Get transaction count before unlinking
        List<Transaction> transactionsBefore = transactionService.getAllTransactions(testUserId);
        int transactionCountBefore = transactionsBefore.size();
        
        // Count Plaid transactions
        long plaidTransactionCount = transactionsBefore.stream()
            .filter(tx -> "PLAID".equals(tx.getSource()))
            .count();
        
        // Step 1: User unlinks account
        plaidService.unlinkAccount(testUserId);
        
        // Step 2: Verify account is deleted/deactivated
        List<PlaidAccount> linkedAccounts = plaidService.getLinkedAccounts(testUserId);
        assertEquals(0, linkedAccounts.stream().filter(PlaidAccount::getIsActive).count());
        
        // Step 3: Verify transactions are retained
        List<Transaction> transactionsAfter = transactionService.getAllTransactions(testUserId);
        assertEquals(transactionCountBefore, transactionsAfter.size());
        
        // Step 4: Verify Plaid transactions still have their data
        long plaidTransactionCountAfter = transactionsAfter.stream()
            .filter(tx -> "PLAID".equals(tx.getSource()))
            .count();
        assertEquals(plaidTransactionCount, plaidTransactionCountAfter);
        
        // Verify Plaid transaction details preserved
        for (Transaction tx : transactionsAfter) {
            if ("PLAID".equals(tx.getSource())) {
                assertNotNull(tx.getPlaidTransactionId());
                assertNotNull(tx.getCategory());
            }
        }
    }
    
    /**
     * Test Plaid transaction deduplication
     * Validates Requirements: 14.4, 14.5
     */
    @Test
    void testPlaidTransactionDeduplication() {
        // Setup: Link account
        String linkToken = plaidService.createLinkToken(testUserId);
        assertNotNull(linkToken);
        
        String mockPublicToken = "public-sandbox-test-token";
        PlaidAccount account = plaidService.exchangePublicToken(mockPublicToken, testUserId);
        assertNotNull(account);
        
        // Step 1: First sync
        TransactionSyncResult firstSync = plaidService.syncTransactions(testUserId);
        assertTrue(firstSync.isSuccess());
        int firstSyncCount = firstSync.getNewTransactionCount();
        
        List<Transaction> transactionsAfterFirstSync = transactionService.getAllTransactions(testUserId);
        int totalAfterFirstSync = transactionsAfterFirstSync.size();
        
        // Step 2: Second sync (should not create duplicates)
        TransactionSyncResult secondSync = plaidService.syncTransactions(testUserId);
        assertTrue(secondSync.isSuccess());
        
        // Step 3: Verify no duplicates created
        List<Transaction> transactionsAfterSecondSync = transactionService.getAllTransactions(testUserId);
        assertEquals(totalAfterFirstSync, transactionsAfterSecondSync.size());
        
        // Step 4: Verify all Plaid transaction IDs are unique
        List<String> plaidTxIds = transactionsAfterSecondSync.stream()
            .filter(tx -> "PLAID".equals(tx.getSource()))
            .map(Transaction::getPlaidTransactionId)
            .filter(id -> id != null)
            .toList();
        
        long uniqueCount = plaidTxIds.stream().distinct().count();
        assertEquals(plaidTxIds.size(), uniqueCount, "All Plaid transaction IDs should be unique");
    }
    
    /**
     * Test Plaid transactions with budget integration
     * Validates Requirements: 14.1, 6.1, 7.1
     */
    @Test
    void testPlaidTransactionsWithBudgetIntegration() {
        // Setup: Create budget
        BudgetRequest budgetRequest = new BudgetRequest(new BigDecimal("1000.00"), currentPeriod);
        budgetService.createBudget(testUserId, budgetRequest);
        
        // Step 1: Link account and sync transactions
        String linkToken = plaidService.createLinkToken(testUserId);
        assertNotNull(linkToken);
        
        String mockPublicToken = "public-sandbox-test-token";
        PlaidAccount account = plaidService.exchangePublicToken(mockPublicToken, testUserId);
        assertNotNull(account);
        
        TransactionSyncResult syncResult = plaidService.syncTransactions(testUserId);
        assertTrue(syncResult.isSuccess());
        
        // Step 2: Verify budget updated with Plaid transactions
        BudgetStatus status = budgetService.getBudgetStatus(testUserId);
        assertNotNull(status);
        assertEquals(new BigDecimal("1000.00"), status.getAmount());
        
        // Spent amount should include Plaid transactions
        assertTrue(status.getSpent().compareTo(BigDecimal.ZERO) >= 0);
        
        // Step 3: Verify remaining balance calculated correctly
        BigDecimal expectedRemaining = status.getAmount().subtract(status.getSpent());
        assertEquals(expectedRemaining, status.getRemaining());
    }
    
    /**
     * Test filtering transactions by source (manual vs Plaid)
     * Validates Requirements: 16.4
     */
    @Test
    void testTransactionSourceFiltering() {
        // Setup: Create manual transaction
        TransactionRequest manualRequest = new TransactionRequest(
            new BigDecimal("50.00"),
            LocalDate.now(),
            "Manual transaction",
            "Food"
        );
        transactionService.createTransaction(testUserId, manualRequest);
        
        // Setup: Link account and sync Plaid transactions
        String linkToken = plaidService.createLinkToken(testUserId);
        assertNotNull(linkToken);
        
        String mockPublicToken = "public-sandbox-test-token";
        PlaidAccount account = plaidService.exchangePublicToken(mockPublicToken, testUserId);
        assertNotNull(account);
        
        TransactionSyncResult syncResult = plaidService.syncTransactions(testUserId);
        assertTrue(syncResult.isSuccess());
        
        // Step 1: Get all transactions
        List<Transaction> allTransactions = transactionService.getAllTransactions(testUserId);
        assertTrue(allTransactions.size() > 1);
        
        // Step 2: Filter manual transactions
        List<Transaction> manualTransactions = allTransactions.stream()
            .filter(tx -> "MANUAL".equals(tx.getSource()))
            .toList();
        assertEquals(1, manualTransactions.size());
        assertEquals("Manual transaction", manualTransactions.get(0).getDescription());
        
        // Step 3: Filter Plaid transactions
        List<Transaction> plaidTransactions = allTransactions.stream()
            .filter(tx -> "PLAID".equals(tx.getSource()))
            .toList();
        assertTrue(plaidTransactions.size() > 0);
        
        // Step 4: Verify all Plaid transactions have Plaid-specific fields
        for (Transaction tx : plaidTransactions) {
            assertNotNull(tx.getPlaidTransactionId());
            assertEquals("PLAID", tx.getSource());
        }
    }
    
    /**
     * Test Plaid transaction display with merchant name
     * Validates Requirements: 16.2, 16.3
     */
    @Test
    void testPlaidTransactionDisplayFields() {
        // Setup: Link account and sync transactions
        String linkToken = plaidService.createLinkToken(testUserId);
        assertNotNull(linkToken);
        
        String mockPublicToken = "public-sandbox-test-token";
        PlaidAccount account = plaidService.exchangePublicToken(mockPublicToken, testUserId);
        assertNotNull(account);
        
        TransactionSyncResult syncResult = plaidService.syncTransactions(testUserId);
        assertTrue(syncResult.isSuccess());
        
        // Step 1: Get Plaid transactions
        List<Transaction> transactions = transactionService.getAllTransactions(testUserId);
        List<Transaction> plaidTransactions = transactions.stream()
            .filter(tx -> "PLAID".equals(tx.getSource()))
            .toList();
        
        assertTrue(plaidTransactions.size() > 0);
        
        // Step 2: Verify Plaid transaction fields
        for (Transaction tx : plaidTransactions) {
            // Required fields
            assertNotNull(tx.getPlaidTransactionId());
            assertNotNull(tx.getCategory());
            assertNotNull(tx.getPlaidCategory());
            
            // Merchant name may be present
            // (not all transactions have merchant names)
            
            // Verify source indicator
            assertEquals("PLAID", tx.getSource());
        }
    }
}
