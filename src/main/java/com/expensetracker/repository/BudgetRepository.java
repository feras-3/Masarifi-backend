package com.expensetracker.repository;

import com.expensetracker.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, String> {
    
    Optional<Budget> findByUserIdAndPeriod(String userId, String period);
    
    Optional<Budget> findFirstByUserIdOrderByCreatedAtDesc(String userId);
}
