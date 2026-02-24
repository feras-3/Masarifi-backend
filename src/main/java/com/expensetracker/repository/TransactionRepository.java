package com.expensetracker.repository;

import com.expensetracker.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    
    List<Transaction> findByUserIdOrderByDateDesc(String userId);
    
    List<Transaction> findByUserIdAndCategory(String userId, String category);
    
    List<Transaction> findByUserIdAndDateBetween(String userId, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT t.category as category, SUM(t.amount) as total " +
           "FROM Transaction t " +
           "WHERE t.userId = :userId " +
           "AND t.date BETWEEN :startDate AND :endDate " +
           "GROUP BY t.category")
    List<Map<String, Object>> getTotalsByCategory(
        @Param("userId") String userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.userId = :userId AND t.date BETWEEN :startDate AND :endDate")
    BigDecimal getTotalSpending(
        @Param("userId") String userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
