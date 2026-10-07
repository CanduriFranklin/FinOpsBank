package com.finopsbank.persistence.repository;

import com.finopsbank.persistence.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SpringDataTransactionRepository extends JpaRepository<TransactionEntity, Long> {
    List<TransactionEntity> findBySourceAccountNumberOrTargetAccountNumberOrderByCreatedAtDesc(String sourceAccount, String targetAccount);
}