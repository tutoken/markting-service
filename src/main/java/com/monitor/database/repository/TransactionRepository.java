package com.monitor.database.repository;

import com.monitor.database.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByType(String type);

    List<Transaction> findByStatus(String status);

    List<Transaction> findByChain(String chain);

    List<Transaction> findByTypeAndStatus(String type, String status);

    List<Transaction> findByTypeAndChain(String type, String chain);

    List<Transaction> findByStatusAndChain(String status, String chain);

    List<Transaction> findByTypeAndStatusAndChain(String type, String status, String chain);

    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :fromTime AND :toTime")
    List<Transaction> findByCreatedAtBetween(long fromTime, long toTime);
}