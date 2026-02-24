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
        
        return transactionRepository.save(transaction);
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
        
        return transactionRepository.save(transaction);
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
