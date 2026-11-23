package com.example.authservice.repository;

import com.example.authservice.model.ParentInvitation;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;

public interface ParentInvitationRepository extends JpaRepository<ParentInvitation, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ParentInvitation i where i.tokenHash = :digest")
    Optional<ParentInvitation> findForAcceptance(String digest);
}
