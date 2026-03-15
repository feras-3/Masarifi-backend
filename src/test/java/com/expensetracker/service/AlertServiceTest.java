package com.expensetracker.service;

import com.expensetracker.model.Alert;
import com.expensetracker.repository.AlertRepository;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AlertService.
 * Covers: ALT-01 through ALT-10 (TDD §4.5)
 * Requirements: FR-ALT-01 through FR-ALT-07
 */
@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AlertService alertService;

    private static final String USER_ID = "user-abc";
    private static final String PERIOD = "2026-03";
    private static final BigDecimal BUDGET = new BigDecimal("1000.00");

    @BeforeEach
    void setUp() {
        when(alertRepository.findByUserIdAndTypeAndPeriod(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
    }

    // ALT-01
    @Test
    void checkBudgetThresholds_Below80_NoAlertCreated() {
        BigDecimal spending = new BigDecimal("799.00"); // 79.9%

        alertService.checkBudgetThresholds(USER_ID, spending, BUDGET, PERIOD);

        verify(alertRepository, never()).save(any());
    }

    // ALT-02
    @Test
    void checkBudgetThresholds_At80_CreatesWarningAlert() {
        BigDecimal spending = new BigDecimal("800.00"); // exactly 80%

        alertService.checkBudgetThresholds(USER_ID, spending, BUDGET, PERIOD);

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, atLeastOnce()).save(captor.capture());
        assertTrue(captor.getAllValues().stream()
                .anyMatch(a -> "WARNING".equals(a.getType())));
    }

    // ALT-03
    @Test
    void checkBudgetThresholds_At100_CreatesBothAlerts() {
        BigDecimal spending = new BigDecimal("1000.00"); // exactly 100%

        alertService.checkBudgetThresholds(USER_ID, spending, BUDGET, PERIOD);

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(2)).save(captor.capture());
        List<Alert> saved = captor.getAllValues();
        assertTrue(saved.stream().anyMatch(a -> "WARNING".equals(a.getType())));
        assertTrue(saved.stream().anyMatch(a -> "CRITICAL".equals(a.getType())));
    }

    // ALT-04
    @Test
    void checkBudgetThresholds_Above100_CreatesBothAlerts() {
        BigDecimal spending = new BigDecimal("1100.00"); // 110%

        alertService.checkBudgetThresholds(USER_ID, spending, BUDGET, PERIOD);

        verify(alertRepository, times(2)).save(any(Alert.class));
    }

    // ALT-05
    @Test
    void checkBudgetThresholds_DuplicateWarning_NotCreatedTwice() {
        BigDecimal spending = new BigDecimal("850.00");
        Alert existing = new Alert(USER_ID, "WARNING", BUDGET, spending,
                new BigDecimal("85.00"), PERIOD);

        // First call — no existing alert
        when(alertRepository.findByUserIdAndTypeAndPeriod(USER_ID, "WARNING", PERIOD))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));

        alertService.checkBudgetThresholds(USER_ID, spending, BUDGET, PERIOD);
        alertService.checkBudgetThresholds(USER_ID, spending, BUDGET, PERIOD);

        // WARNING should only be saved once
        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, atLeastOnce()).save(captor.capture());
        long warningCount = captor.getAllValues().stream()
                .filter(a -> "WARNING".equals(a.getType())).count();
        assertEquals(1, warningCount);
    }

    // ALT-06
    @Test
    void checkBudgetThresholds_ZeroBudget_NoAlertCreated() {
        alertService.checkBudgetThresholds(USER_ID, new BigDecimal("100.00"),
                BigDecimal.ZERO, PERIOD);

        verify(alertRepository, never()).save(any());
    }

    // ALT-07
    @Test
    void dismissAlert_MarksAlertAsDismissed() {
        Alert alert = new Alert(USER_ID, "WARNING", BUDGET,
                new BigDecimal("850.00"), new BigDecimal("85.00"), PERIOD);
        alert.setId("alert-001");
        when(alertRepository.findById("alert-001")).thenReturn(Optional.of(alert));

        alertService.dismissAlert("alert-001");

        assertTrue(alert.isDismissed());
        verify(alertRepository).save(alert);
    }

    // ALT-08
    @Test
    void dismissAlert_WhenNotFound_ThrowsException() {
        when(alertRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> alertService.dismissAlert("unknown"));
    }

    // ALT-09
    @Test
    void getUnreadAlertCount_ReturnsOnlyUndismissed() {
        when(alertRepository.countByUserIdAndDismissedFalse(USER_ID)).thenReturn(2L);

        long count = alertService.getUnreadAlertCount(USER_ID);

        assertEquals(2L, count);
    }

    // ALT-10
    @Test
    void getActiveAlerts_ExcludesDismissed() {
        Alert active = new Alert(USER_ID, "WARNING", BUDGET,
                new BigDecimal("850.00"), new BigDecimal("85.00"), PERIOD);
        when(alertRepository.findByUserIdAndDismissedFalse(USER_ID)).thenReturn(List.of(active));

        List<Alert> result = alertService.getActiveAlerts(USER_ID);

        assertEquals(1, result.size());
        assertFalse(result.get(0).isDismissed());
    }
}
