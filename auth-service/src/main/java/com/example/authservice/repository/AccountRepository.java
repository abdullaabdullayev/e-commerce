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

    Optional<Account> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    Optional<Account> findByUsername(String username);
    boolean existsByUsername(String username);

    @Query("select count(a) from Account a join a.roles r where r = :role")
    long countWithRole(Role role);

    @Query("select count(a) from Account a join a.roles r where r = :role and a.active = true")
    long countActiveWithRole(Role role);

    @Query(value = "select 1 from pg_advisory_xact_lock(82350001)", nativeQuery = true)
    int lockRoleChanges();

    @Query("""
        select a.id from Account a
        where (:allRoles = true or :role member of a.roles)
        and (:term = '' or locate(lower(:term), lower(a.email)) > 0
             or locate(lower(:term), lower(a.username)) > 0 or str(a.id) = :term)
        order by a.id
        """)
    List<Long> searchIds(String term, boolean allRoles, Role role, Pageable page);

    default Optional<Account> findByIdentifier(String identifier) {
        // Emails are matched case-insensitively so accounts created before normalization still log in.
        return identifier.contains("@") ? findByEmailIgnoreCase(identifier.trim()) : findByUsername(identifier);
    }
}
