package com.example.userservice.service;

import com.example.userservice.integration.FamilyAccountDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FamilyAssignmentService {
    private final FamilyAccountDirectory accounts;
    private final FamilyService families;

    public void assign(String token, long parentId, long studentId) {
        accounts.requireRole(token, parentId, "PARENT");
        accounts.requireRole(token, studentId, "STUDENT");
        families.link(parentId, studentId);
    }
}
