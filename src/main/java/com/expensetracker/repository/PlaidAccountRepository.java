package com.expensetracker.repository;

import com.expensetracker.model.PlaidAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaidAccountRepository extends JpaRepository<PlaidAccount, String> {
    
    List<PlaidAccount> findByUserId(String userId);
    
    List<PlaidAccount> findByUserIdAndIsActive(String userId, Boolean isActive);
    
    Optional<PlaidAccount> findByUserIdAndItemId(String userId, String itemId);
    
    Optional<PlaidAccount> findFirstByUserIdAndIsActiveOrderByLinkedAtDesc(String userId, Boolean isActive);
}
