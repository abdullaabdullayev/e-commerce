package com.example.authservice.repository;

import com.example.authservice.model.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RefreshToken t join fetch t.account where t.token = :token")
    Optional<RefreshToken> lockByToken(String token);

    @Modifying
    @Query("delete from RefreshToken t where t.token = :token")
    int deleteByToken(String token);

    @Modifying
    @Query("delete from RefreshToken t where t.account.id = :accountId")
    int deleteByAccountId(long accountId);

    @Modifying
    @Query("delete from RefreshToken t where t.expiryDate < :now")
    int deleteExpired(Instant now);
}
