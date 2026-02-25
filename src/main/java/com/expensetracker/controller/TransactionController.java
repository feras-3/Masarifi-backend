package com.expensetracker.controller;

import com.expensetracker.model.Transaction;
import com.expensetracker.model.TransactionRequest;
import com.expensetracker.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {
    
    @Autowired
    private TransactionService transactionService;
    
    /**
     * POST /api/transactions - Create a new transaction
     * Requirements: 2.1, 2.6, 9.1
     */
    @PostMapping
    public ResponseEntity<Transaction> createTransaction(@Valid @RequestBody TransactionRequest request) {
        String userId = getAuthenticatedUserId();
        Transaction transaction = transactionService.createTransaction(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }
    
    /**
     * GET /api/transactions - Retrieve all transactions for the authenticated user
     * Requirements: 3.1, 9.1
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTransactions() {
        String userId = getAuthenticatedUserId();
        List<Transaction> transactions = transactionService.getAllTransactions(userId);
        
        BigDecimal total = transactions.stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Map<String, Object> response = new HashMap<>();
        response.put("transactions", transactions);
        response.put("total", total);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * PUT /api/transactions/{id} - Update an existing transaction
     * Requirements: 4.1, 9.1
     */
    @PutMapping("/{id}")
    public ResponseEntity<Transaction> updateTransaction(
            @PathVariable String id,
            @Valid @RequestBody TransactionRequest request) {
        String userId = getAuthenticatedUserId();
        Transaction transaction = transactionService.updateTransaction(id, userId, request);
        return ResponseEntity.ok(transaction);
    }
    
    /**
     * DELETE /api/transactions/{id} - Delete a transaction
     * Requirements: 4.2, 9.1
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Boolean>> deleteTransaction(@PathVariable String id) {
        String userId = getAuthenticatedUserId();
        transactionService.deleteTransaction(id, userId);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("success", true);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /api/transactions/by-category - Get spending totals by category
     * Requirements: 5.3, 5.5, 9.1
     */
    @GetMapping("/by-category")
    public ResponseEntity<Map<String, Object>> getTransactionsByCategory(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        String userId = getAuthenticatedUserId();
        
        // If category is specified, return filtered transactions
        if (category != null && !category.isEmpty()) {
            List<Transaction> transactions = transactionService.getTransactionsByCategory(userId, category);
            Map<String, Object> response = new HashMap<>();
            response.put("transactions", transactions);
            return ResponseEntity.ok(response);
        }
        
        // Otherwise, return category totals
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().withDayOfMonth(1);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        
        Map<String, BigDecimal> categoryTotals = transactionService.getTotalsByCategory(userId, start, end);
        
        Map<String, Object> response = new HashMap<>();
        response.put("categoryTotals", categoryTotals);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Helper method to get the authenticated user ID from SecurityContext
     */
    private String getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
