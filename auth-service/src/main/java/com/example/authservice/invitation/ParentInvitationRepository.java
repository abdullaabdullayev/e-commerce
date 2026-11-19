package com.example.authservice.invitation;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;

interface ParentInvitationRepository extends JpaRepository<ParentInvitation, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ParentInvitation i where i.tokenHash = :digest")
    Optional<ParentInvitation> findForAcceptance(String digest);
}
