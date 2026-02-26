package com.expensetracker.service;

import com.expensetracker.model.Transaction;
import com.expensetracker.model.TransactionRequest;
import com.expensetracker.repository.TransactionRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@Validated
public class TransactionService {
    
    private static final Set<String> VALID_CATEGORIES = Set.of(
        "Food", "Transportation", "Entertainment", "Utilities", 
        "Healthcare", "Shopping", "Other"
    );
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private BudgetService budgetService;
    
    @Transactional
    public Transaction createTransaction(String userId, @Valid TransactionRequest request) {
        validateCategory(request.getCategory());
        
        Transaction transaction = new Transaction(
            userId,
            request.getAmount(),
            request.getDate(),
            request.getDescription(),
            request.getCategory()
        );
        
        Transaction saved = transactionRepository.save(transaction);
        
        // Trigger budget recalculation and alert checking (only if budget exists)
        recalculateBudgetIfExists(userId);
        
        return saved;
    }
    
    /**
     * Recalculate budget balance if a budget exists for the user.
     * This method checks if a budget exists before calling the budget service
     * to avoid transaction rollback issues.
     */
    private void recalculateBudgetIfExists(String userId) {
        try {
            budgetService.recalculateBalance(userId);
        } catch (NoSuchElementException e) {
            // No budget exists yet, that's okay - skip recalculation
        } catch (Exception e) {
            // Log other exceptions but don't fail the transaction
            System.err.println("Warning: Failed to recalculate budget for user " + userId + ": " + e.getMessage());
        }
    }
    
    public List<Transaction> getAllTransactions(String userId) {
        return transactionRepository.findByUserIdOrderByDateDesc(userId);
    }
    
    @Transactional
    public Transaction updateTransaction(String id, String userId, @Valid TransactionRequest request) {
        validateCategory(request.getCategory());
        
        Transaction transaction = transactionRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Transaction with id " + id + " not found"));
        
        // Verify the transaction belongs to the user
        if (!transaction.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Transaction does not belong to user");
        }
        
        transaction.setAmount(request.getAmount());
        transaction.setDate(request.getDate());
        transaction.setDescription(request.getDescription());
        transaction.setCategory(request.getCategory());
        
        Transaction updated = transactionRepository.save(transaction);
        
        // Trigger budget recalculation and alert checking (only if budget exists)
        recalculateBudgetIfExists(userId);
        
        return updated;
    }
    
    @Transactional
    public void deleteTransaction(String id, String userId) {
        Transaction transaction = transactionRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Transaction with id " + id + " not found"));
        
        // Verify the transaction belongs to the user
        if (!transaction.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Transaction does not belong to user");
        }
        
        transactionRepository.delete(transaction);
        
        // Trigger budget recalculation and alert checking (only if budget exists)
        recalculateBudgetIfExists(userId);
    }
    
    public Map<String, BigDecimal> getTotalsByCategory(String userId, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> results = transactionRepository.getTotalsByCategory(userId, startDate, endDate);
        
        Map<String, BigDecimal> totals = new HashMap<>();
        for (Map<String, Object> result : results) {
            String category = (String) result.get("category");
            BigDecimal total = (BigDecimal) result.get("total");
            totals.put(category, total);
        }
        
        return totals;
    }
    
    public List<Transaction> getTransactionsByCategory(String userId, String category) {
        validateCategory(category);
        return transactionRepository.findByUserIdAndCategory(userId, category);
    }
    
    private void validateCategory(String category) {
        if (!VALID_CATEGORIES.contains(category)) {
            throw new IllegalArgumentException(
                "Invalid category. Must be one of: " + String.join(", ", VALID_CATEGORIES)
            );
        }
    }
}
