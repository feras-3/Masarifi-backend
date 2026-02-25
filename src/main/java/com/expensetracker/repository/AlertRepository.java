package com.expensetracker.repository;

import com.expensetracker.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, String> {
    
    /**
     * Find all alerts for a specific user
     */
    List<Alert> findByUserId(String userId);
    
    /**
     * Find all alerts for a specific user and period
     */
    List<Alert> findByUserIdAndPeriod(String userId, String period);
    
    /**
     * Find a specific alert by user, type, and period
     * This is used to check if an alert already exists before creating a new one
     */
    Optional<Alert> findByUserIdAndTypeAndPeriod(String userId, String type, String period);
    
    /**
     * Find all non-dismissed alerts for a user
     */
    List<Alert> findByUserIdAndDismissedFalse(String userId);
    
    /**
     * Count non-dismissed alerts for a user
     */
    long countByUserIdAndDismissedFalse(String userId);
}
