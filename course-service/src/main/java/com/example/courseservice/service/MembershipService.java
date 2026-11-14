package com.example.courseservice.service;

import com.example.courseservice.security.Actor;
import com.example.courseservice.domain.EnrollmentStatus;
import com.example.courseservice.integration.AuthAccountClient;
import com.example.courseservice.integration.ProfileClient;
import org.springframework.stereotype.Service;
import java.util.UUID;
import static com.example.courseservice.dto.LearningDtos.*;

@Service
public class MembershipService {
    private final AuthAccountClient accounts;
    private final ProfileClient profiles;
    private final MembershipTransactions transactions;

    public MembershipService(AuthAccountClient accounts, ProfileClient profiles, MembershipTransactions transactions) {
        this.accounts = accounts;
        this.profiles = profiles;
        this.transactions = transactions;
    }

    // Network calls deliberately occur outside database transactions.
    public void assignTeacher(Actor actor, String token, UUID courseId, long accountId) {
        actor.requireAdmin();
        accounts.requireRole(token, accountId, "TEACHER");
        transactions.assignTeacher(actor, courseId, accountId);
    }

    public void removeTeacher(Actor actor, UUID courseId, long accountId) {
        transactions.removeTeacher(actor, courseId, accountId);
    }

    public EnrollmentResponse enroll(Actor actor, String token, UUID courseId, long accountId, EnrollmentStatus status) {
        actor.requireAdmin();
        if (status == EnrollmentStatus.ACTIVE) accounts.requireRole(token, accountId, "STUDENT");
        return transactions.enroll(actor, courseId, accountId, status);
    }

    public RosterResponse roster(Actor actor, UUID courseId, int page) {
        return profiles.enrich(transactions.roster(actor, courseId, page));
    }
}
