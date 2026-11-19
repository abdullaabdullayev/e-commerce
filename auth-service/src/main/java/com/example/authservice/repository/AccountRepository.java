package com.example.authservice.repository;

import com.example.authservice.model.Account;
import com.example.authservice.model.Role;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface AccountRepository extends  JpaRepository<Account, Long> {

    Optional<Account> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<Account> findByUsername(String username);
    boolean existsByUsername(String username);

    @Query("select a.id from Account a order by a.id")
    List<Long> listIds(Pageable page);

    @Query("select a.id from Account a join a.roles r where r = :role order by a.id")
    List<Long> listIdsWithRole(Role role, Pageable page);

    default Optional<Account> findByIdentifier(String identifier) {
        return identifier.contains("@") ? findByEmail(identifier) : findByUsername(identifier);
    }
}
