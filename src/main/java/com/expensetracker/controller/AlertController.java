package com.expensetracker.controller;

import com.expensetracker.model.Alert;
import com.expensetracker.service.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "*")
public class AlertController {
    
    @Autowired
    private AlertService alertService;
    
    /**
     * GET /api/alerts - Get all alerts for the authenticated user
     * Requirements: 7.5, 9.1
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAlerts() {
        String userId = getAuthenticatedUserId();
        List<Alert> alerts = alertService.getAlerts(userId);
        long unreadCount = alertService.getUnreadAlertCount(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("alerts", alerts);
        response.put("unreadCount", unreadCount);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * PUT /api/alerts/{id}/dismiss - Dismiss an alert
     * Requirements: 8.3, 9.1
     */
    @PutMapping("/{id}/dismiss")
    public ResponseEntity<Map<String, Boolean>> dismissAlert(@PathVariable String id) {
        alertService.dismissAlert(id);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("success", true);
        
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
