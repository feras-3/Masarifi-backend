package com.expensetracker.service;

import com.expensetracker.model.Budget;
import com.expensetracker.model.BudgetRequest;
import com.expensetracker.model.BudgetStatus;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.TransactionRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Validated
public class BudgetService {
    
    @Autowired
    private BudgetRepository budgetRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private AlertService alertService;
    
    @Transactional
    public Budget createBudget(String userId, @Valid BudgetRequest request) {
        // Convert period to YYYY-MM format if it's a period type like "MONTHLY"
        String period = normalizePeriod(request.getPeriod());
        
        // Check if a budget already exists for this user and period
        budgetRepository.findByUserIdAndPeriod(userId, period).ifPresent(existingBudget -> {
            // Delete the existing budget to overwrite it
            budgetRepository.delete(existingBudget);
        });
        
        Budget budget = new Budget(userId, request.getAmount(), period);
        return budgetRepository.save(budget);
    }
    
    /**
     * Normalize period input to YYYY-MM format.
     * Accepts either "MONTHLY", "WEEKLY", etc. (converts to current month)
     * or direct YYYY-MM format.
     */
    private String normalizePeriod(String period) {
        if (period == null || period.isEmpty()) {
            throw new IllegalArgumentException("Period is required");
        }
        
        // If it's already in YYYY-MM format, return as is
        if (period.matches("\\d{4}-\\d{2}")) {
            return period;
        }
        
        // Convert period types to current month format
        if (period.equalsIgnoreCase("MONTHLY") || 
            period.equalsIgnoreCase("WEEKLY") || 
            period.equalsIgnoreCase("DAILY") || 
            period.equalsIgnoreCase("YEARLY")) {
            // Use current year-month
            return YearMonth.now().toString();
        }
        
        throw new IllegalArgumentException("Invalid period format. Use 'MONTHLY' or 'YYYY-MM' format (e.g., '2026-02')");
    }
    
    public Budget getCurrentBudget(String userId) {
        return budgetRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
            .orElseThrow(() -> new NoSuchElementException("User doesn't have a budget yet"));
    }
    
    /**
     * Get all budgets for a user with their status
     */
    public List<BudgetStatus> getAllBudgets(String userId) {
        List<Budget> budgets = budgetRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        if (budgets.isEmpty()) {
            throw new NoSuchElementException("User doesn't have any budgets yet");
        }
        
        return budgets.stream()
            .map(budget -> calculateBudgetStatus(budget, userId))
            .toList();
    }
    
    /**
     * Get a specific budget by ID
     */
    public BudgetStatus getBudgetById(String id, String userId) {
        Budget budget = budgetRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Budget with id " + id + " not found"));
        
        // Verify the budget belongs to the user
        if (!budget.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Budget does not belong to user");
        }
        
        return calculateBudgetStatus(budget, userId);
    }
    
    /**
     * Calculate budget status for a given budget
     */
    private BudgetStatus calculateBudgetStatus(Budget budget, String userId) {
        // Parse the period to get start and end dates
        LocalDate[] periodDates = parsePeriod(budget.getPeriod());
        LocalDate startDate = periodDates[0];
        LocalDate endDate = periodDates[1];
        
        // Calculate spent amount
        BigDecimal spent = transactionRepository.getTotalSpending(userId, startDate, endDate);
        if (spent == null) {
            spent = BigDecimal.ZERO;
        }
        
        // Calculate remaining and percentage
        BigDecimal remaining = budget.getAmount().subtract(spent);
        BigDecimal percentageUsed = BigDecimal.ZERO;
        if (budget.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            percentageUsed = spent.divide(budget.getAmount(), 4, RoundingMode.HALF_UP)
                                  .multiply(new BigDecimal("100"))
                                  .setScale(2, RoundingMode.HALF_UP);
        }
        
        return new BudgetStatus(
            budget.getId(),
            budget.getAmount(),
            spent,
            remaining,
            percentageUsed,
            budget.getPeriod()
        );
    }
    
    @Transactional
    public Budget updateBudget(String id, String userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        
        Budget budget = budgetRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Budget with id " + id + " not found"));
        
        // Verify the budget belongs to the user
        if (!budget.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Budget does not belong to user");
        }
        
        budget.setAmount(amount);
        return budgetRepository.save(budget);
    }
    
    public BudgetStatus getBudgetStatus(String userId) {
        Budget budget = budgetRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
            .orElseThrow(() -> new NoSuchElementException("User doesn't have a budget yet"));
        
        return calculateBudgetStatus(budget, userId);
    }
    
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void recalculateBalance(String userId) {
        try {
            // This method is called after transaction changes to trigger any necessary updates
            // Uses REQUIRES_NEW to avoid marking parent transaction for rollback if this fails
            Budget budget = getCurrentBudget(userId);
            
            // Parse the period to get start and end dates
            LocalDate[] periodDates = parsePeriod(budget.getPeriod());
            LocalDate startDate = periodDates[0];
            LocalDate endDate = periodDates[1];
            
            // Calculate spent amount
            BigDecimal spent = transactionRepository.getTotalSpending(userId, startDate, endDate);
            if (spent == null) {
                spent = BigDecimal.ZERO;
            }
            
            System.out.println("=== Budget Recalculation ===");
            System.out.println("User: " + userId);
            System.out.println("Budget Amount: " + budget.getAmount());
            System.out.println("Current Spending: " + spent);
            System.out.println("Period: " + budget.getPeriod());
            
            // Check budget thresholds and generate alerts if needed
            alertService.checkBudgetThresholds(userId, spent, budget.getAmount(), budget.getPeriod());
            
            System.out.println("Alert check completed");
            System.out.println("===========================");
        } catch (Exception e) {
            System.err.println("Error in recalculateBalance: " + e.getMessage());
            e.printStackTrace();
            throw e;
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
}
