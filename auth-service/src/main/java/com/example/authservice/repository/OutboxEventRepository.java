package com.example.authservice.repository;

import com.example.authservice.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    // Each relay locks only one outbox row; concurrent instances skip that row.
    @Query(value = """
            SELECT * FROM auth_outbox
            WHERE published_at IS NULL AND next_attempt_at <= CURRENT_TIMESTAMP
            ORDER BY created_at, id
            LIMIT 1 FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<OutboxEvent> lockNextPending();
}
