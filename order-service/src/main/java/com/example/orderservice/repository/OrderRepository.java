package com.example.orderservice.repository;

import com.example.orderservice.model.CustomerOrder;
import com.example.orderservice.model.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import java.time.Instant;
import java.util.*;

public interface OrderRepository extends JpaRepository<CustomerOrder, UUID> {
    @Query(value = "select 1 from pg_advisory_xact_lock(hashtextextended(:key, 0))", nativeQuery = true)
    int lockCheckout(String key);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.id = :id")
    Optional<CustomerOrder> lockById(UUID id);

    Optional<CustomerOrder> findByIdAndCustomerSubject(UUID id, String subject);
    List<CustomerOrder> findByCustomerSubjectOrderByCreatedAtDescIdAsc(String subject, Pageable pageable);

    @Query("""
        select o.id from CustomerOrder o where o.status in :pending
        or (o.status = com.example.orderservice.model.OrderStatus.PENDING_PAYMENT and o.expiresAt <= :now)
        order by o.updatedAt, o.id
        """)
    List<UUID> work(Collection<OrderStatus> pending, Instant now, Pageable pageable);
}
