package com.expensetracker.service;

import com.expensetracker.model.Alert;
import com.expensetracker.repository.AlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class AlertService {
    
    @Autowired
    private AlertRepository alertRepository;
    
    private static final String WARNING_TYPE = "WARNING";
    private static final String CRITICAL_TYPE = "CRITICAL";
    private static final BigDecimal WARNING_THRESHOLD = new BigDecimal("80");
    private static final BigDecimal CRITICAL_THRESHOLD = new BigDecimal("100");
    
    /**
     * Check budget thresholds and generate alerts if needed
     * Generates WARNING alert at 80% and CRITICAL alert at 100%
     * Ensures alerts are generated only once per threshold per period
     */
    @Transactional
    public void checkBudgetThresholds(String userId, BigDecimal currentSpending, 
                                     BigDecimal budgetAmount, String period) {
        if (budgetAmount == null || budgetAmount.compareTo(BigDecimal.ZERO) == 0) {
            return; // No budget set, no alerts to generate
        }
        
        // Calculate percentage of budget used
        BigDecimal percentageUsed = currentSpending
            .divide(budgetAmount, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"))
            .setScale(2, RoundingMode.HALF_UP);
        
        // Check for CRITICAL alert (100% or more)
        if (percentageUsed.compareTo(CRITICAL_THRESHOLD) >= 0) {
            generateAlertIfNotExists(userId, CRITICAL_TYPE, budgetAmount, currentSpending, 
                                    percentageUsed, period);
        }
        
        // Check for WARNING alert (80% or more)
        if (percentageUsed.compareTo(WARNING_THRESHOLD) >= 0) {
            generateAlertIfNotExists(userId, WARNING_TYPE, budgetAmount, currentSpending, 
                                    percentageUsed, period);
        }
    }
    
    /**
     * Generate an alert if it doesn't already exist for the user, type, and period
     */
    private void generateAlertIfNotExists(String userId, String type, BigDecimal budgetAmount,
                                         BigDecimal currentSpending, BigDecimal percentageUsed,
                                         String period) {
        // Check if alert already exists
        Optional<Alert> existingAlert = alertRepository.findByUserIdAndTypeAndPeriod(userId, type, period);
        
        if (existingAlert.isEmpty()) {
            // Create new alert
            Alert alert = new Alert(userId, type, budgetAmount, currentSpending, 
                                   percentageUsed, period);
            alertRepository.save(alert);
        }
    }
    
    /**
     * Retrieve all alerts for a user
     */
    public List<Alert> getAlerts(String userId) {
        return alertRepository.findByUserId(userId);
    }
    
    /**
     * Retrieve only non-dismissed alerts for a user
     */
    public List<Alert> getActiveAlerts(String userId) {
        return alertRepository.findByUserIdAndDismissedFalse(userId);
    }
    
    /**
     * Get count of unread (non-dismissed) alerts
     */
    public long getUnreadAlertCount(String userId) {
        return alertRepository.countByUserIdAndDismissedFalse(userId);
    }
    
    /**
     * Dismiss an alert by marking it as dismissed
     */
    @Transactional
    public void dismissAlert(String alertId) {
        Alert alert = alertRepository.findById(alertId)
            .orElseThrow(() -> new NoSuchElementException("Alert with id " + alertId + " not found"));
        
        alert.setDismissed(true);
        alertRepository.save(alert);
    }
}
