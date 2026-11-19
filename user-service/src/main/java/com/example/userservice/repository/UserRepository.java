package com.example.userservice.repository;

import com.example.userservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByAccountId(Long accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.accountId = :accountId")
    Optional<User> lockByAccountId(long accountId);
    java.util.List<User> findByAccountIdIn(java.util.Collection<Long> accountIds);

    @Modifying
    @Query(value = """
            INSERT INTO users (account_id, email, first_name, last_name)
            VALUES (:accountId, :email, :firstName, :lastName)
            ON CONFLICT (account_id) DO NOTHING
            """, nativeQuery = true)
    int insertProfileIfAbsent(@Param("accountId") Long accountId,
                              @Param("email") String email,
                              @Param("firstName") String firstName,
                              @Param("lastName") String lastName);
}
