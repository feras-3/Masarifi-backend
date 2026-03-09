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

        @Query("SELECT t FROM Transaction t WHERE t.userId = :userId AND LOWER(t.category) = LOWER(:category)")
        List<Transaction> findByUserIdAndCategory(@Param("userId") String userId, @Param("category") String category);

        List<Transaction> findByUserIdAndDateBetween(String userId, LocalDate startDate, LocalDate endDate);

        @Query("SELECT t.category as category, SUM(t.amount) as total " +
                        "FROM Transaction t " +
                        "WHERE t.userId = :userId " +
                        "AND t.date BETWEEN :startDate AND :endDate " +
                        "GROUP BY t.category")
        List<Map<String, Object>> getTotalsByCategory(
                        @Param("userId") String userId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.userId = :userId AND t.date BETWEEN :startDate AND :endDate")
        BigDecimal getTotalSpending(
                        @Param("userId") String userId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.userId = :userId AND LOWER(t.category) = LOWER(:category) AND t.date BETWEEN :startDate AND :endDate")
        BigDecimal getTotalSpendingByCategory(
                        @Param("userId") String userId,
                        @Param("category") String category,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        java.util.Optional<Transaction> findByPlaidTransactionId(String plaidTransactionId);
}
