package com.expensetracker.service;

import com.expensetracker.model.*;
import com.expensetracker.repository.AlertRepository;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.TransactionRepository;
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
 * Integration test to verify that transaction creation/update/deletion
 * properly triggers budget recalculation and alert checking.
 * 
 * Validates Requirements: 6.1, 7.1, 7.2
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TransactionBudgetIntegrationTest {
    
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
        currentPeriod = LocalDate.now().toString().substring(0, 7); // YYYY-MM format
        
        // Clean up any existing data for test user
        transactionRepository.deleteAll();
        budgetRepository.deleteAll();
        alertRepository.deleteAll();
    }
    
    @Test
    void testTransactionCreationTriggersBudgetRecalculation() {
        // Given: A budget of $1000
        BudgetRequest budgetRequest = new BudgetRequest(new BigDecimal("1000.00"), currentPeriod);
        budgetService.createBudget(testUserId, budgetRequest);
        
        // When: Creating a transaction of $100
        TransactionRequest transactionRequest = new TransactionRequest(
            new BigDecimal("100.00"),
            LocalDate.now(),
            "Test transaction",
            "Food"
        );
        transactionService.createTransaction(testUserId, transactionRequest);
        
        // Then: Budget status should reflect the spending
        BudgetStatus status = budgetService.getBudgetStatus(testUserId);
        assertEquals(new BigDecimal("1000.00"), status.getAmount());
        assertEquals(new BigDecimal("100.00"), status.getSpent());
        assertEquals(new BigDecimal("900.00"), status.getRemaining());
    }
    
    @Test
    void testTransactionCreationTriggersWarningAlertAt80Percent() {
        // Given: A budget of $1000
        BudgetRequest budgetRequest = new BudgetRequest(new BigDecimal("1000.00"), currentPeriod);
        budgetService.createBudget(testUserId, budgetRequest);
        
        // When: Creating transactions totaling $800 (80% of budget)
        for (int i = 0; i < 8; i++) {
            TransactionRequest transactionRequest = new TransactionRequest(
                new BigDecimal("100.00"),
                LocalDate.now(),
                "Test transaction " + i,
                "Food"
            );
            transactionService.createTransaction(testUserId, transactionRequest);
        }
        
        // Then: A warning alert should be generated
        List<Alert> alerts = alertService.getAlerts(testUserId);
        assertEquals(1, alerts.size());
        assertEquals("WARNING", alerts.get(0).getType());
        assertFalse(alerts.get(0).isDismissed());
    }
    
    @Test
    void testTransactionCreationTriggersCriticalAlertAt100Percent() {
        // Given: A budget of $1000
        BudgetRequest budgetRequest = new BudgetRequest(new BigDecimal("1000.00"), currentPeriod);
        budgetService.createBudget(testUserId, budgetRequest);
        
        // When: Creating transactions totaling $1000 (100% of budget)
        for (int i = 0; i < 10; i++) {
            TransactionRequest transactionRequest = new TransactionRequest(
                new BigDecimal("100.00"),
                LocalDate.now(),
                "Test transaction " + i,
                "Food"
            );
            transactionService.createTransaction(testUserId, transactionRequest);
        }
        
        // Then: Both warning and critical alerts should be generated
        List<Alert> alerts = alertService.getAlerts(testUserId);
        assertEquals(2, alerts.size());
        
        boolean hasWarning = alerts.stream().anyMatch(a -> "WARNING".equals(a.getType()));
        boolean hasCritical = alerts.stream().anyMatch(a -> "CRITICAL".equals(a.getType()));
        
        assertTrue(hasWarning, "Should have warning alert");
        assertTrue(hasCritical, "Should have critical alert");
    }
    
    @Test
    void testTransactionUpdateTriggersBudgetRecalculation() {
        // Given: A budget and a transaction
        BudgetRequest budgetRequest = new BudgetRequest(new BigDecimal("1000.00"), currentPeriod);
        budgetService.createBudget(testUserId, budgetRequest);
        
        TransactionRequest initialRequest = new TransactionRequest(
            new BigDecimal("100.00"),
            LocalDate.now(),
            "Initial transaction",
            "Food"
        );
        Transaction transaction = transactionService.createTransaction(testUserId, initialRequest);
        
        // When: Updating the transaction amount to $200
        TransactionRequest updateRequest = new TransactionRequest(
            new BigDecimal("200.00"),
            LocalDate.now(),
            "Updated transaction",
            "Food"
        );
        transactionService.updateTransaction(transaction.getId(), testUserId, updateRequest);
        
        // Then: Budget status should reflect the updated spending
        BudgetStatus status = budgetService.getBudgetStatus(testUserId);
        assertEquals(new BigDecimal("200.00"), status.getSpent());
        assertEquals(new BigDecimal("800.00"), status.getRemaining());
    }
    
    @Test
    void testTransactionDeletionTriggersBudgetRecalculation() {
        // Given: A budget and two transactions
        BudgetRequest budgetRequest = new BudgetRequest(new BigDecimal("1000.00"), currentPeriod);
        budgetService.createBudget(testUserId, budgetRequest);
        
        TransactionRequest request1 = new TransactionRequest(
            new BigDecimal("100.00"),
            LocalDate.now(),
            "Transaction 1",
            "Food"
        );
        Transaction transaction1 = transactionService.createTransaction(testUserId, request1);
        
        TransactionRequest request2 = new TransactionRequest(
            new BigDecimal("200.00"),
            LocalDate.now(),
            "Transaction 2",
            "Food"
        );
        transactionService.createTransaction(testUserId, request2);
        
        // Verify initial state
        BudgetStatus initialStatus = budgetService.getBudgetStatus(testUserId);
        assertEquals(new BigDecimal("300.00"), initialStatus.getSpent());
        
        // When: Deleting one transaction
        transactionService.deleteTransaction(transaction1.getId(), testUserId);
        
        // Then: Budget status should reflect the reduced spending
        BudgetStatus finalStatus = budgetService.getBudgetStatus(testUserId);
        assertEquals(new BigDecimal("200.00"), finalStatus.getSpent());
        assertEquals(new BigDecimal("800.00"), finalStatus.getRemaining());
    }
    
    @Test
    void testMultipleTransactionChangesUpdateAlertsCorrectly() {
        // Given: A budget of $1000
        BudgetRequest budgetRequest = new BudgetRequest(new BigDecimal("1000.00"), currentPeriod);
        budgetService.createBudget(testUserId, budgetRequest);
        
        // When: Creating transactions that cross the 80% threshold
        for (int i = 0; i < 7; i++) {
            TransactionRequest transactionRequest = new TransactionRequest(
                new BigDecimal("100.00"),
                LocalDate.now(),
                "Transaction " + i,
                "Food"
            );
            transactionService.createTransaction(testUserId, transactionRequest);
        }
        
        // Then: No alert yet (70% spent)
        List<Alert> alertsBefore = alertService.getAlerts(testUserId);
        assertEquals(0, alertsBefore.size());
        
        // When: Adding one more transaction to reach 80%
        TransactionRequest transactionRequest = new TransactionRequest(
            new BigDecimal("100.00"),
            LocalDate.now(),
            "Transaction 8",
            "Food"
        );
        transactionService.createTransaction(testUserId, transactionRequest);
        
        // Then: Warning alert should be generated
        List<Alert> alertsAfter = alertService.getAlerts(testUserId);
        assertEquals(1, alertsAfter.size());
        assertEquals("WARNING", alertsAfter.get(0).getType());
    }
    
    @Test
    void testTransactionCreationWithoutBudgetDoesNotFail() {
        // Given: No budget exists
        
        // When: Creating a transaction
        TransactionRequest transactionRequest = new TransactionRequest(
            new BigDecimal("100.00"),
            LocalDate.now(),
            "Test transaction",
            "Food"
        );
        
        // Then: Transaction creation should succeed without throwing exception
        assertDoesNotThrow(() -> {
            Transaction transaction = transactionService.createTransaction(testUserId, transactionRequest);
            assertNotNull(transaction);
            assertNotNull(transaction.getId());
        });
    }
}
