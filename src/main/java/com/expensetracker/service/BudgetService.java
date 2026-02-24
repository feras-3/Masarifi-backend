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
import java.util.NoSuchElementException;

@Service
@Validated
public class BudgetService {
    
    @Autowired
    private BudgetRepository budgetRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Transactional
    public Budget createBudget(String userId, @Valid BudgetRequest request) {
        Budget budget = new Budget(userId, request.getAmount(), request.getPeriod());
        return budgetRepository.save(budget);
    }
    
    public Budget getCurrentBudget(String userId) {
        return budgetRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
            .orElseThrow(() -> new NoSuchElementException("No budget found for user"));
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
    public void recalculateBalance(String userId) {
        // This method is called after transaction changes to trigger any necessary updates
        // The actual balance calculation is done on-demand in getBudgetStatus
        // This method can be used to trigger alerts or other side effects in the future
        
        // For now, we just verify the budget exists
        getCurrentBudget(userId);
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
