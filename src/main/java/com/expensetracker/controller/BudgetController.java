package com.expensetracker.controller;

import com.expensetracker.model.Budget;
import com.expensetracker.model.BudgetRequest;
import com.expensetracker.model.BudgetStatus;
import com.expensetracker.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Map;

@RestController
@RequestMapping("/api/budgets")
@CrossOrigin(origins = "*")
public class BudgetController {
    
    @Autowired
    private BudgetService budgetService;
    
    /**
     * POST /api/budgets - Create a new budget
     * Requirements: 1.1, 9.1
     */
    @PostMapping
    public ResponseEntity<Budget> createBudget(@Valid @RequestBody BudgetRequest request) {
        String userId = getAuthenticatedUserId();
        Budget budget = budgetService.createBudget(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(budget);
    }
    
    /**
     * GET /api/budgets/current - Get all budgets for the authenticated user
     * Requirements: 1.4, 6.3, 9.1
     */
    @GetMapping("/current")
    public ResponseEntity<List<BudgetStatus>> getAllBudgets() {
        String userId = getAuthenticatedUserId();
        List<BudgetStatus> budgets = budgetService.getAllBudgets(userId);
        return ResponseEntity.ok(budgets);
    }
    
    /**
     * GET /api/budgets/{id} - Get budget by ID
     * Requirements: 1.4, 9.1
     */
    @GetMapping("/{id}")
    public ResponseEntity<BudgetStatus> getBudgetById(@PathVariable String id) {
        String userId = getAuthenticatedUserId();
        BudgetStatus budgetStatus = budgetService.getBudgetById(id, userId);
        return ResponseEntity.ok(budgetStatus);
    }
    
    /**
     * PUT /api/budgets/{id} - Update an existing budget
     * Requirements: 1.3, 9.1
     */
    @PutMapping("/{id}")
    public ResponseEntity<Budget> updateBudget(
            @PathVariable String id,
            @RequestBody Map<String, BigDecimal> request) {
        String userId = getAuthenticatedUserId();
        BigDecimal amount = request.get("amount");
        Budget budget = budgetService.updateBudget(id, userId, amount);
        return ResponseEntity.ok(budget);
    }
    
    /**
     * Helper method to get the authenticated user ID from SecurityContext
     */
    private String getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
