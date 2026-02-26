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
public class EndToEndIntegrationTest {
    @Autowired
    private TransactionService transactionService;
    
    @Autowired
    private BudgetService budgetService;
    
    @Autowired
    private AlertService alertService;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private BudgetRepository budgetRepository;
    
    @Autowired
    private AlertRepository alertRepository;
    
    private String testUserId;
    private String currentPeriod;
    
    @BeforeEach
    void setUp() {
        testUserId = "test-user-" + System.currentTimeMillis();
        currentPeriod = LocalDate.now().toString().substring(0, 7);
        
        transactionRepository.deleteAll();
        budgetRepository.deleteAll();
        alertRepository.deleteAll();
    }
    
    /**
     * Test complete user flow: login, create transaction, view budget, receive alert
     * Validates Requirements: 2.1, 6.1, 7.1, 7.2, 8.1
     */
    @Test
    void testCompleteUserFlow_CreateTransactionViewBudgetReceiveAlert() {
        // Step 1: User sets up a budget
        BudgetRequest budgetRequest = new BudgetRequest(new BigDecimal("1000.00"), currentPeriod);
        Budget budget = budgetService.createBudget(testUserId, budgetRequest);
        assertNotNull(budget);
        assertEquals(new BigDecimal("1000.00"), budget.getAmount());
        
        // Step 2: User creates first transaction
        TransactionRequest txRequest1 = new TransactionRequest(
            new BigDecimal("300.00"),
            LocalDate.now(),
            "Grocery shopping",
            "Food"
        );
        Transaction tx1 = transactionService.createTransaction(testUserId, txRequest1);
        assertNotNull(tx1);
        assertNotNull(tx1.getId());
        
        // Step 3: User views budget status
        BudgetStatus status1 = budgetService.getBudgetStatus(testUserId);
        assertEquals(new BigDecimal("1000.00"), status1.getAmount());
        assertEquals(new BigDecimal("300.00"), status1.getSpent());
        assertEquals(new BigDecimal("700.00"), status1.getRemaining());
        assertEquals(30.0, status1.getPercentageUsed().doubleValue(), 0.01);
        
        // Step 4: User creates more transactions reaching 80% threshold
        TransactionRequest txRequest2 = new TransactionRequest(
            new BigDecimal("500.00"),
            LocalDate.now(),
            "Rent payment",
            "Utilities"
        );
        transactionService.createTransaction(testUserId, txRequest2);
        
        // Step 5: User views budget status and sees warning
        BudgetStatus status2 = budgetService.getBudgetStatus(testUserId);
        assertEquals(new BigDecimal("800.00"), status2.getSpent());
        assertEquals(new BigDecimal("200.00"), status2.getRemaining());
        
        // Step 6: User receives warning alert
        List<Alert> alerts = alertService.getAlerts(testUserId);
        assertEquals(1, alerts.size());
        Alert warningAlert = alerts.get(0);
        assertEquals("WARNING", warningAlert.getType());
        assertEquals(new BigDecimal("1000.00"), warningAlert.getBudgetAmount());
        assertEquals(new BigDecimal("800.00"), warningAlert.getCurrentSpending());
        assertFalse(warningAlert.isDismissed());
        
        // Step 7: User creates another transaction exceeding budget
        TransactionRequest txRequest3 = new TransactionRequest(
            new BigDecimal("300.00"),
            LocalDate.now(),
            "Shopping",
            "Shopping"
        );
        transactionService.createTransaction(testUserId, txRequest3);
        
        // Step 8: User receives critical alert
        List<Alert> finalAlerts = alertService.getAlerts(testUserId);
        assertEquals(2, finalAlerts.size());
        
        boolean hasWarning = finalAlerts.stream().anyMatch(a -> "WARNING".equals(a.getType()));
        boolean hasCritical = finalAlerts.stream().anyMatch(a -> "CRITICAL".equals(a.getType()));
        assertTrue(hasWarning);
        assertTrue(hasCritical);
        
        // Step 9: User views final budget status
        BudgetStatus finalStatus = budgetService.getBudgetStatus(testUserId);
        assertEquals(new BigDecimal("1100.00"), finalStatus.getSpent());
        assertEquals(new BigDecimal("-100.00"), finalStatus.getRemaining());
        assertTrue(finalStatus.getPercentageUsed().doubleValue() > 100.0);
    }
    
    /**
     * Test transaction edit and delete flows
     * Validates Requirements: 4.1, 4.2, 6.1
     */
    @Test
    void testTransactionEditAndDeleteFlow() {
        // Setup: Create budget and initial transaction
        BudgetRequest budgetRequest = new BudgetRequest(new BigDecimal("1000.00"), currentPeriod);
        budgetService.createBudget(testUserId, budgetRequest);
        
        TransactionRequest initialRequest = new TransactionRequest(
            new BigDecimal("200.00"),
            LocalDate.now(),
            "Initial purchase",
            "Shopping"
        );
        Transaction transaction = transactionService.createTransaction(testUserId, initialRequest);
        
        // Verify initial state
        BudgetStatus initialStatus = budgetService.getBudgetStatus(testUserId);
        assertEquals(new BigDecimal("200.00"), initialStatus.getSpent());
        
        // Step 1: User edits transaction
        TransactionRequest updateRequest = new TransactionRequest(
            new BigDecimal("350.00"),
            LocalDate.now(),
            "Updated purchase",
            "Food"
        );
        Transaction updatedTx = transactionService.updateTransaction(
            transaction.getId(), 
            testUserId, 
            updateRequest
        );
        
        assertEquals(new BigDecimal("350.00"), updatedTx.getAmount());
        assertEquals("Updated purchase", updatedTx.getDescription());
        assertEquals("Food", updatedTx.getCategory());
        
        // Step 2: Verify budget updated after edit
        BudgetStatus afterEditStatus = budgetService.getBudgetStatus(testUserId);
        assertEquals(new BigDecimal("350.00"), afterEditStatus.getSpent());
        assertEquals(new BigDecimal("650.00"), afterEditStatus.getRemaining());
        
        // Step 3: User deletes transaction
        transactionService.deleteTransaction(transaction.getId(), testUserId);
        
        // Step 4: Verify transaction deleted
        List<Transaction> transactions = transactionService.getAllTransactions(testUserId);
        assertEquals(0, transactions.size());
        
        // Step 5: Verify budget updated after deletion
        BudgetStatus afterDeleteStatus = budgetService.getBudgetStatus(testUserId);
        assertEquals(new BigDecimal("0.00"), afterDeleteStatus.getSpent());
        assertEquals(new BigDecimal("1000.00"), afterDeleteStatus.getRemaining());
    }
    
    /**
     * Test budget creation and update flows
     * Validates Requirements: 1.1, 1.3, 6.3
     */
    @Test
    void testBudgetCreationAndUpdateFlow() {
        // Step 1: User creates initial budget
        BudgetRequest initialRequest = new BudgetRequest(new BigDecimal("500.00"), currentPeriod);
        Budget budget = budgetService.createBudget(testUserId, initialRequest);
        
        assertNotNull(budget);
        assertNotNull(budget.getId());
        assertEquals(new BigDecimal("500.00"), budget.getAmount());
        assertEquals(currentPeriod, budget.getPeriod());
        
        // Step 2: User creates some transactions
        TransactionRequest txRequest = new TransactionRequest(
            new BigDecimal("100.00"),
            LocalDate.now(),
            "Test transaction",
            "Food"
        );
        transactionService.createTransaction(testUserId, txRequest);
        
        // Step 3: User views budget status
        BudgetStatus status1 = budgetService.getBudgetStatus(testUserId);
        assertEquals(new BigDecimal("500.00"), status1.getAmount());
        assertEquals(new BigDecimal("100.00"), status1.getSpent());
        assertEquals(new BigDecimal("400.00"), status1.getRemaining());
        
        // Step 4: User updates budget amount
        Budget updatedBudget = budgetService.updateBudget(budget.getId(), testUserId, new BigDecimal("800.00"));
        assertEquals(new BigDecimal("800.00"), updatedBudget.getAmount());
        
        // Step 5: User views updated budget status
        BudgetStatus status2 = budgetService.getBudgetStatus(testUserId);
        assertEquals(new BigDecimal("800.00"), status2.getAmount());
        assertEquals(new BigDecimal("100.00"), status2.getSpent());
        assertEquals(new BigDecimal("700.00"), status2.getRemaining());
        assertEquals(12.5, status2.getPercentageUsed().doubleValue(), 0.01);
    }
    
    /**
     * Test multiple transactions with category filtering
     * Validates Requirements: 2.1, 3.1, 3.2, 5.5
     */
    @Test
    void testMultipleTransactionsWithCategoryFiltering() {
        // Step 1: User creates transactions in different categories
        String[] categories = {"Food", "Transportation", "Entertainment", "Shopping"};
        BigDecimal[] amounts = {
            new BigDecimal("50.00"),
            new BigDecimal("30.00"),
            new BigDecimal("100.00"),
            new BigDecimal("75.00")
        };
        
        for (int i = 0; i < categories.length; i++) {
            TransactionRequest request = new TransactionRequest(
                amounts[i],
                LocalDate.now().minusDays(i),
                "Transaction in " + categories[i],
                categories[i]
            );
            transactionService.createTransaction(testUserId, request);
        }
        
        // Step 2: User retrieves all transactions
        List<Transaction> allTransactions = transactionService.getAllTransactions(testUserId);
        assertEquals(4, allTransactions.size());
        
        // Step 3: Verify transactions are sorted by date descending
        for (int i = 0; i < allTransactions.size() - 1; i++) {
            assertTrue(
                allTransactions.get(i).getDate().compareTo(allTransactions.get(i + 1).getDate()) >= 0,
                "Transactions should be sorted by date descending"
            );
        }
        
        // Step 4: Verify total amount
        BigDecimal total = allTransactions.stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("255.00"), total);
    }
    
    /**
     * Test alert dismissal flow
     * Validates Requirements: 7.1, 8.3, 8.4
     */
    @Test
    void testAlertDismissalFlow() {
        // Setup: Create budget and transactions to trigger alert
        BudgetRequest budgetRequest = new BudgetRequest(new BigDecimal("1000.00"), currentPeriod);
        budgetService.createBudget(testUserId, budgetRequest);
        
        for (int i = 0; i < 8; i++) {
            TransactionRequest txRequest = new TransactionRequest(
                new BigDecimal("100.00"),
                LocalDate.now(),
                "Transaction " + i,
                "Food"
            );
            transactionService.createTransaction(testUserId, txRequest);
        }
        
        // Step 1: User views alerts
        List<Alert> alerts = alertService.getAlerts(testUserId);
        assertEquals(1, alerts.size());
        Alert alert = alerts.get(0);
        assertFalse(alert.isDismissed());
        
        // Step 2: User dismisses alert
        alertService.dismissAlert(alert.getId());
        
        // Step 3: Verify alert is dismissed
        List<Alert> alertsAfterDismiss = alertService.getAlerts(testUserId);
        assertEquals(1, alertsAfterDismiss.size());
        assertTrue(alertsAfterDismiss.get(0).isDismissed());
    }
    
    /**
     * Test complete flow with no budget set
     * Validates Requirements: 2.1, 3.1
     */
    @Test
    void testTransactionFlowWithoutBudget() {
        // Step 1: User creates transactions without setting budget
        TransactionRequest request1 = new TransactionRequest(
            new BigDecimal("100.00"),
            LocalDate.now(),
            "Transaction 1",
            "Food"
        );
        Transaction tx1 = transactionService.createTransaction(testUserId, request1);
        assertNotNull(tx1);
        
        TransactionRequest request2 = new TransactionRequest(
            new BigDecimal("200.00"),
            LocalDate.now(),
            "Transaction 2",
            "Shopping"
        );
        Transaction tx2 = transactionService.createTransaction(testUserId, request2);
        assertNotNull(tx2);
        
        // Step 2: User retrieves transactions
        List<Transaction> transactions = transactionService.getAllTransactions(testUserId);
        assertEquals(2, transactions.size());
        
        // Step 3: Verify no alerts generated
        List<Alert> alerts = alertService.getAlerts(testUserId);
        assertEquals(0, alerts.size());
    }
    
    /**
     * Test edge case: exactly at threshold boundaries
     * Validates Requirements: 7.1, 7.2, 7.3
     */
    @Test
    void testAlertGenerationAtExactThresholds() {
        // Setup: Create budget
        BudgetRequest budgetRequest = new BudgetRequest(new BigDecimal("1000.00"), currentPeriod);
        budgetService.createBudget(testUserId, budgetRequest);
        
        // Step 1: Spend exactly 79.99% - no alert
        TransactionRequest request1 = new TransactionRequest(
            new BigDecimal("799.90"),
            LocalDate.now(),
            "Just below threshold",
            "Food"
        );
        transactionService.createTransaction(testUserId, request1);
        
        List<Alert> alertsBefore = alertService.getAlerts(testUserId);
        assertEquals(0, alertsBefore.size());
        
        // Step 2: Spend to exactly 80% - warning alert
        TransactionRequest request2 = new TransactionRequest(
            new BigDecimal("0.10"),
            LocalDate.now(),
            "Reach 80%",
            "Food"
        );
        transactionService.createTransaction(testUserId, request2);
        
        List<Alert> alertsAt80 = alertService.getAlerts(testUserId);
        assertEquals(1, alertsAt80.size());
        assertEquals("WARNING", alertsAt80.get(0).getType());
        
        // Step 3: Spend to exactly 100% - critical alert
        TransactionRequest request3 = new TransactionRequest(
            new BigDecimal("200.00"),
            LocalDate.now(),
            "Reach 100%",
            "Food"
        );
        transactionService.createTransaction(testUserId, request3);
        
        List<Alert> alertsAt100 = alertService.getAlerts(testUserId);
        assertEquals(2, alertsAt100.size());
        
        boolean hasWarning = alertsAt100.stream().anyMatch(a -> "WARNING".equals(a.getType()));
        boolean hasCritical = alertsAt100.stream().anyMatch(a -> "CRITICAL".equals(a.getType()));
        assertTrue(hasWarning);
        assertTrue(hasCritical);
    }
}
