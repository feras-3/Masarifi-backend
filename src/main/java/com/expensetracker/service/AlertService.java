package com.expensetracker.service;

import com.expensetracker.model.Alert;
import com.expensetracker.model.Budget;
import com.expensetracker.repository.AlertRepository;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class AlertService {
    
    @Autowired
    private AlertRepository alertRepository;
    
    @Autowired
    private BudgetRepository budgetRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
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
        System.out.println("=== Checking Budget Thresholds ===");
        System.out.println("User: " + userId);
        System.out.println("Current Spending: " + currentSpending);
        System.out.println("Budget Amount: " + budgetAmount);
        System.out.println("Period: " + period);
        
        if (budgetAmount == null || budgetAmount.compareTo(BigDecimal.ZERO) == 0) {
            System.out.println("No budget set, skipping alert generation");
            System.out.println("==================================");
            return; // No budget set, no alerts to generate
        }
        
        // Calculate percentage of budget used
        BigDecimal percentageUsed = currentSpending
            .divide(budgetAmount, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"))
            .setScale(2, RoundingMode.HALF_UP);
        
        System.out.println("Percentage Used: " + percentageUsed + "%");
        
        // Check for CRITICAL alert (100% or more)
        if (percentageUsed.compareTo(CRITICAL_THRESHOLD) >= 0) {
            System.out.println("CRITICAL threshold reached, generating alert...");
            generateAlertIfNotExists(userId, CRITICAL_TYPE, budgetAmount, currentSpending, 
                                    percentageUsed, period);
        }
        
        // Check for WARNING alert (80% or more)
        if (percentageUsed.compareTo(WARNING_THRESHOLD) >= 0) {
            System.out.println("WARNING threshold reached, generating alert...");
            generateAlertIfNotExists(userId, WARNING_TYPE, budgetAmount, currentSpending, 
                                    percentageUsed, period);
        }
        
        System.out.println("==================================");
    }
    
    /**
     * Generate an alert if it doesn't already exist for the user, type, and period
     */
    private void generateAlertIfNotExists(String userId, String type, BigDecimal budgetAmount,
                                         BigDecimal currentSpending, BigDecimal percentageUsed,
                                         String period) {
        System.out.println("Checking for existing " + type + " alert...");
        
        // Check if alert already exists
        Optional<Alert> existingAlert = alertRepository.findByUserIdAndTypeAndPeriod(userId, type, period);
        
        if (existingAlert.isEmpty()) {
            System.out.println("No existing alert found, creating new " + type + " alert");
            // Create new alert
            Alert alert = new Alert(userId, type, budgetAmount, currentSpending, 
                                   percentageUsed, period);
            Alert saved = alertRepository.save(alert);
            System.out.println("Alert created with ID: " + saved.getId());
        } else {
            System.out.println("Alert already exists, skipping creation");
        }
    }
    
    /**
     * Retrieve all alerts for a user
     * Also checks all budgets and generates any missing alerts
     */
    public List<Alert> getAlerts(String userId) {
        // First, check all budgets and generate any missing alerts
        checkAllBudgetsForAlerts(userId);
        
        // Then return all alerts
        return alertRepository.findByUserId(userId);
    }
    
    /**
     * Check all budgets for a user and generate alerts if thresholds are met
     */
    @Transactional
    public void checkAllBudgetsForAlerts(String userId) {
        try {
            System.out.println("=== Checking All Budgets for Alerts ===");
            System.out.println("User: " + userId);
            
            // Get all budgets for the user
            List<Budget> budgets = budgetRepository.findByUserIdOrderByCreatedAtDesc(userId);
            
            if (budgets.isEmpty()) {
                System.out.println("No budgets found for user");
                System.out.println("======================================");
                return;
            }
            
            System.out.println("Found " + budgets.size() + " budget(s)");
            
            // Check each budget
            for (Budget budget : budgets) {
                System.out.println("\nChecking budget: " + budget.getId() + " for period " + budget.getPeriod());
                
                // Parse the period to get start and end dates
                LocalDate[] periodDates = parsePeriod(budget.getPeriod());
                LocalDate startDate = periodDates[0];
                LocalDate endDate = periodDates[1];
                
                // Calculate spent amount for this period
                BigDecimal spent = transactionRepository.getTotalSpending(userId, startDate, endDate);
                if (spent == null) {
                    spent = BigDecimal.ZERO;
                }
                
                System.out.println("Budget amount: " + budget.getAmount());
                System.out.println("Current spending: " + spent);
                
                // Check thresholds for this budget
                checkBudgetThresholds(userId, spent, budget.getAmount(), budget.getPeriod());
            }
            
            System.out.println("\n======================================");
        } catch (Exception e) {
            System.err.println("Error checking budgets for alerts: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Parse a period string (e.g., "2024-01") into start and end dates
     */
    private LocalDate[] parsePeriod(String period) {
        try {
            YearMonth yearMonth = YearMonth.parse(period);
            LocalDate startDate = yearMonth.atDay(1);
            LocalDate endDate = yearMonth.atEndOfMonth();
            return new LocalDate[]{startDate, endDate};
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid period format. Expected format: YYYY-MM");
        }
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
