package com.expensetracker.service;

import com.expensetracker.model.Budget;
import com.expensetracker.model.BudgetRequest;
import com.expensetracker.model.BudgetStatus;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BudgetService.
 * These tests verify the core budget operations including creation, retrieval,
 * updates, and status calculations.
 */
@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private BudgetService budgetService;

    private String testUserId;
    private BudgetRequest validBudgetRequest;
    private Budget testBudget;

    @BeforeEach
    void setUp() {
        testUserId = "user-123";
        validBudgetRequest = new BudgetRequest(new BigDecimal("1000.00"), "2024-01");
        testBudget = new Budget(testUserId, new BigDecimal("1000.00"), "2024-01");
        testBudget.setId("budget-123");
    }

    @Test
    void createBudget_WithValidData_ShouldSucceed() {
        // Given
        when(budgetRepository.save(any(Budget.class))).thenReturn(testBudget);

        // When
        Budget result = budgetService.createBudget(testUserId, validBudgetRequest);

        // Then
        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertEquals(new BigDecimal("1000.00"), result.getAmount());
        assertEquals("2024-01", result.getPeriod());
        verify(budgetRepository, times(1)).save(any(Budget.class));
    }

    @Test
    void getCurrentBudget_WhenBudgetExists_ShouldReturnBudget() {
        // Given
        when(budgetRepository.findFirstByUserIdOrderByCreatedAtDesc(testUserId))
                .thenReturn(Optional.of(testBudget));

        // When
        Budget result = budgetService.getCurrentBudget(testUserId);

        // Then
        assertNotNull(result);
        assertEquals(testBudget.getId(), result.getId());
        assertEquals(testUserId, result.getUserId());
    }

    @Test
    void getCurrentBudget_WhenNoBudgetExists_ShouldThrowException() {
        // Given
        when(budgetRepository.findFirstByUserIdOrderByCreatedAtDesc(testUserId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(NoSuchElementException.class, () -> {
            budgetService.getCurrentBudget(testUserId);
        });
    }

    @Test
    void updateBudget_WithValidAmount_ShouldUpdateAndReturn() {
        // Given
        String budgetId = "budget-123";
        BigDecimal newAmount = new BigDecimal("1500.00");
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(testBudget));
        when(budgetRepository.save(any(Budget.class))).thenReturn(testBudget);

        // When
        Budget result = budgetService.updateBudget(budgetId, testUserId, newAmount);

        // Then
        assertNotNull(result);
        verify(budgetRepository, times(1)).save(testBudget);
    }

    @Test
    void updateBudget_WithNonPositiveAmount_ShouldThrowException() {
        // Given
        String budgetId = "budget-123";
        BigDecimal invalidAmount = BigDecimal.ZERO;

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            budgetService.updateBudget(budgetId, testUserId, invalidAmount);
        });
    }

    @Test
    void updateBudget_WhenBudgetNotFound_ShouldThrowException() {
        // Given
        String budgetId = "non-existent";
        BigDecimal newAmount = new BigDecimal("1500.00");
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NoSuchElementException.class, () -> {
            budgetService.updateBudget(budgetId, testUserId, newAmount);
        });
    }

    @Test
    void updateBudget_WhenBudgetBelongsToOtherUser_ShouldThrowException() {
        // Given
        String budgetId = "budget-123";
        String otherUserId = "other-user";
        BigDecimal newAmount = new BigDecimal("1500.00");
        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(testBudget));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            budgetService.updateBudget(budgetId, otherUserId, newAmount);
        });
    }

    @Test
    void getBudgetStatus_WithNoTransactions_ShouldReturnFullBudget() {
        // Given
        when(budgetRepository.findFirstByUserIdOrderByCreatedAtDesc(testUserId))
                .thenReturn(Optional.of(testBudget));
        when(transactionRepository.getTotalSpending(eq(testUserId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(null); // No transactions

        // When
        BudgetStatus status = budgetService.getBudgetStatus(testUserId);

        // Then
        assertNotNull(status);
        assertEquals(testBudget.getId(), status.getBudgetId());
        assertEquals(new BigDecimal("1000.00"), status.getAmount());
        assertEquals(BigDecimal.ZERO, status.getSpent());
        assertEquals(new BigDecimal("1000.00"), status.getRemaining());
        assertEquals(new BigDecimal("0.00"), status.getPercentageUsed());
    }

    @Test
    void getBudgetStatus_WithTransactions_ShouldCalculateCorrectly() {
        // Given
        BigDecimal spent = new BigDecimal("300.00");
        when(budgetRepository.findFirstByUserIdOrderByCreatedAtDesc(testUserId))
                .thenReturn(Optional.of(testBudget));
        when(transactionRepository.getTotalSpending(eq(testUserId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(spent);

        // When
        BudgetStatus status = budgetService.getBudgetStatus(testUserId);

        // Then
        assertNotNull(status);
        assertEquals(new BigDecimal("1000.00"), status.getAmount());
        assertEquals(new BigDecimal("300.00"), status.getSpent());
        assertEquals(new BigDecimal("700.00"), status.getRemaining());
        assertEquals(new BigDecimal("30.00"), status.getPercentageUsed());
    }

    @Test
    void getBudgetStatus_WhenSpendingExceedsBudget_ShouldShowNegativeRemaining() {
        // Given
        BigDecimal spent = new BigDecimal("1200.00");
        when(budgetRepository.findFirstByUserIdOrderByCreatedAtDesc(testUserId))
                .thenReturn(Optional.of(testBudget));
        when(transactionRepository.getTotalSpending(eq(testUserId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(spent);

        // When
        BudgetStatus status = budgetService.getBudgetStatus(testUserId);

        // Then
        assertNotNull(status);
        assertEquals(new BigDecimal("1000.00"), status.getAmount());
        assertEquals(new BigDecimal("1200.00"), status.getSpent());
        assertEquals(new BigDecimal("-200.00"), status.getRemaining());
        assertEquals(new BigDecimal("120.00"), status.getPercentageUsed());
    }

    @Test
    void recalculateBalance_WhenBudgetExists_ShouldNotThrowException() {
        // Given
        when(budgetRepository.findFirstByUserIdOrderByCreatedAtDesc(testUserId))
                .thenReturn(Optional.of(testBudget));

        // When & Then
        assertDoesNotThrow(() -> {
            budgetService.recalculateBalance(testUserId);
        });
    }

    @Test
    void recalculateBalance_WhenNoBudgetExists_ShouldThrowException() {
        // Given
        when(budgetRepository.findFirstByUserIdOrderByCreatedAtDesc(testUserId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(NoSuchElementException.class, () -> {
            budgetService.recalculateBalance(testUserId);
        });
    }
}
