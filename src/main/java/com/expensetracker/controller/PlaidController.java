package com.expensetracker.controller;

import com.expensetracker.model.PlaidAccount;
import com.expensetracker.model.TransactionSyncResult;
import com.expensetracker.model.WebhookRequest;
import com.expensetracker.service.PlaidService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plaid")
@CrossOrigin(origins = "*")
public class PlaidController {
    
    @Autowired
    private PlaidService plaidService;
    
    /**
     * POST /api/plaid/public-token - Create a public token for linking a bank account
     * Uses configured Plaid credentials from application.properties
     * Requirements: 13.1, 9.1
     */
    @PostMapping("/public-token")
    public ResponseEntity<Map<String, String>> createPublicToken(
            @RequestBody Map<String, String> request) {
        String institutionId = request.get("institution_id");
        
        if (institutionId == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "institutionId is required"));
        }
        
        String publicToken = plaidService.createPublicToken(institutionId);
        
        Map<String, String> response = new HashMap<>();
        response.put("publicToken", publicToken);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /api/plaid/exchange-token - Exchange public token for access token
     * Requirements: 13.5, 9.1
     */
    @PostMapping("/exchange-token")
    public ResponseEntity<Map<String, Object>> exchangePublicToken(
            @RequestBody Map<String, String> request) {
        String userId = getAuthenticatedUserId();
        String publicToken = request.get("publicToken");
        
        PlaidAccount account = plaidService.exchangePublicToken(publicToken, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("accountId", account.getId());
        response.put("institutionName", account.getInstitutionName());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * POST /api/plaid/sync-transactions - Manually trigger transaction sync
     * Requirements: 14.1, 14.7, 9.1
     */
    @PostMapping("/sync-transactions")
    public ResponseEntity<TransactionSyncResult> syncTransactions() {
        String userId = getAuthenticatedUserId();
        TransactionSyncResult result = plaidService.syncTransactions(userId);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * GET /api/plaid/accounts - Get linked Plaid accounts
     * Requirements: 17.5, 9.1
     */
    @GetMapping("/accounts")
    public ResponseEntity<Map<String, Object>> getLinkedAccounts() {
        String userId = getAuthenticatedUserId();
        List<PlaidAccount> accounts = plaidService.getLinkedAccounts(userId);
        
        boolean linked = !accounts.isEmpty() && accounts.stream().anyMatch(PlaidAccount::getIsActive);
        
        Map<String, Object> response = new HashMap<>();
        response.put("accounts", accounts);
        response.put("linked", linked);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * DELETE /api/plaid/unlink - Unlink Plaid account
     * Requirements: 17.5, 9.1
     */
    @DeleteMapping("/unlink")
    public ResponseEntity<Map<String, Boolean>> unlinkAccount() {
        String userId = getAuthenticatedUserId();
        plaidService.unlinkAccount(userId);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("success", true);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /api/plaid/webhook - Handle Plaid webhook notifications (optional)
     * Requirements: 18.1
     * Note: This endpoint does not require authentication as it's called by Plaid
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Boolean>> handleWebhook(@RequestBody WebhookRequest webhookRequest) {
        plaidService.handleWebhook(webhookRequest);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("acknowledged", true);
        
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
