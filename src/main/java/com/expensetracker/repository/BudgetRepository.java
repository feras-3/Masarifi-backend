package com.expensetracker.repository;

import com.expensetracker.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, String> {

    Optional<Budget> findFirstByUserIdAndPeriod(String userId, String period);

    @Query("SELECT b FROM Budget b WHERE b.userId = :userId AND b.period = :period AND LOWER(b.category) = LOWER(:category)")
    Optional<Budget> findByUserIdAndPeriodAndCategory(@Param("userId") String userId, @Param("period") String period,
            @Param("category") String category);

    Optional<Budget> findFirstByUserIdOrderByCreatedAtDesc(String userId);

    List<Budget> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Budget> findByUserIdAndPeriod(String userId, String period);
}
