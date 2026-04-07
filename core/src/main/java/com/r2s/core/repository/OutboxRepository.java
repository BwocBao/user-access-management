package com.r2s.core.repository;

import com.r2s.core.entity.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<Outbox, String> {

    List<Outbox> findTop10ByStatusOrderByCreatedAtAsc(String status);

    @Transactional
    @Modifying
    @Query("""
        UPDATE Outbox o
        SET o.status = 'PROCESSING'
        WHERE o.id = :id AND o.status = 'PENDING'
    """)
    int markAsProcessing(String id);

    @Transactional
    @Modifying
    @Query("""
        UPDATE Outbox o
        SET o.status = 'SENT'
        WHERE o.id = :id AND o.status = 'PROCESSING'
    """)
    int markAsSent(String id);

    @Transactional
    @Modifying
    @Query("""
        UPDATE Outbox o
        SET o.retryCount = o.retryCount + 1,
            o.status = 'PENDING'
        WHERE o.id = :id AND o.status = 'PROCESSING'
    """)
    int increaseRetryAndRequeue(String id);

    @Transactional
    @Modifying
    @Query("""
        UPDATE Outbox o
        SET o.status = 'FAILED'
        WHERE o.id = :id
    """)
    int markAsFailed(String id);
}