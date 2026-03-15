package com.expensetracker.service;

import com.expensetracker.model.Transaction;
import com.expensetracker.model.TransactionRequest;
import com.expensetracker.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TransactionService.
 * Covers: TS-01 through TS-12 (TDD §4.4)
 * Requirements: FR-TXN-01 through FR-TXN-08
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BudgetService budgetService;

    @InjectMocks
    private TransactionService transactionService;

    private static final String USER_ID = "user-123";
    private TransactionRequest validRequest;
    private Transaction savedTransaction;

    @BeforeEach
    void setUp() {
        validRequest = new TransactionRequest(
                new BigDecimal("50.00"), LocalDate.now(), "Groceries", "Food");

        savedTransaction = new Transaction(USER_ID, new BigDecimal("50.00"),
                LocalDate.now(), "Groceries", "Food");
        savedTransaction.setId("tx-001");
    }

    // TS-01
    @Test
    void createTransaction_WithValidData_ReturnsSavedTransaction() {
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        Transaction result = transactionService.createTransaction(USER_ID, validRequest);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("MANUAL", result.getSource());
        verify(transactionRepository).save(any(Transaction.class));
    }

    // TS-02
    @Test
    void createTransaction_WithInvalidCategory_ThrowsException() {
        TransactionRequest badRequest = new TransactionRequest(
                new BigDecimal("10.00"), LocalDate.now(), "desc", "InvalidCategory");

        assertThrows(IllegalArgumentException.class,
                () -> transactionService.createTransaction(USER_ID, badRequest));
        verify(transactionRepository, never()).save(any());
    }

    // TS-03
    @Test
    void createTransaction_TriggersRecalculation() {
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        transactionService.createTransaction(USER_ID, validRequest);

        verify(budgetService).recalculateBalance(USER_ID);
    }

    // TS-04
    @Test
    void getAllTransactions_ReturnsUserTransactions() {
        when(transactionRepository.findByUserIdOrderByDateDesc(USER_ID))
                .thenReturn(List.of(savedTransaction));

        List<Transaction> result = transactionService.getAllTransactions(USER_ID);

        assertEquals(1, result.size());
        assertEquals(USER_ID, result.get(0).getUserId());
    }

    // TS-05
    @Test
    void updateTransaction_WithValidData_ReturnsUpdated() {
        TransactionRequest updateReq = new TransactionRequest(
                new BigDecimal("75.00"), LocalDate.now(), "Updated", "Entertainment");
        when(transactionRepository.findById("tx-001")).thenReturn(Optional.of(savedTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        Transaction result = transactionService.updateTransaction("tx-001", USER_ID, updateReq);

        assertNotNull(result);
        verify(transactionRepository).save(savedTransaction);
    }

    // TS-06
    @Test
    void updateTransaction_WhenNotOwner_ThrowsException() {
        when(transactionRepository.findById("tx-001")).thenReturn(Optional.of(savedTransaction));

        assertThrows(IllegalArgumentException.class,
                () -> transactionService.updateTransaction("tx-001", "other-user", validRequest));
    }

    // TS-07
    @Test
    void updateTransaction_WhenNotFound_ThrowsException() {
        when(transactionRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> transactionService.updateTransaction("unknown", USER_ID, validRequest));
    }

    // TS-08
    @Test
    void deleteTransaction_WhenOwner_DeletesSuccessfully() {
        when(transactionRepository.findById("tx-001")).thenReturn(Optional.of(savedTransaction));

        transactionService.deleteTransaction("tx-001", USER_ID);

        verify(transactionRepository).delete(savedTransaction);
    }

    // TS-09
    @Test
    void deleteTransaction_WhenNotOwner_ThrowsException() {
        when(transactionRepository.findById("tx-001")).thenReturn(Optional.of(savedTransaction));

        assertThrows(IllegalArgumentException.class,
                () -> transactionService.deleteTransaction("tx-001", "other-user"));
        verify(transactionRepository, never()).delete(any());
    }

    // TS-10
    @Test
    void deleteTransaction_WhenNotFound_ThrowsException() {
        when(transactionRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> transactionService.deleteTransaction("unknown", USER_ID));
    }

    // TS-11
    @Test
    void getTransactionsByCategory_WithValidCategory_ReturnsList() {
        when(transactionRepository.findByUserIdAndCategory(USER_ID, "Food"))
                .thenReturn(List.of(savedTransaction));

        List<Transaction> result = transactionService.getTransactionsByCategory(USER_ID, "Food");

        assertEquals(1, result.size());
    }

    // TS-12
    @Test
    void getTransactionsByCategory_WithInvalidCategory_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> transactionService.getTransactionsByCategory(USER_ID, "Junk"));
    }
}
