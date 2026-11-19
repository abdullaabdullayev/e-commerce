package com.example.userservice.family;

import com.example.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class FamilyService {
    private final UserRepository users;
    private final FamilyRepository links;

    List<ChildProfile> children(long parentId) {
        return links.children(parentId);
    }

    void requireLink(long parentId, long studentId) {
        if (!links.existsByParentAccountIdAndStudentAccountId(parentId, studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    @Transactional
    public void link(long parentId, long studentId) {
        if (parentId == studentId) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        users.lockByAccountId(parentId).orElseThrow(FamilyService::profilePending);
        users.findByAccountId(studentId).orElseThrow(FamilyService::profilePending);
        if (!links.existsByParentAccountIdAndStudentAccountId(parentId, studentId)) {
            links.save(new ParentStudentLink(parentId, studentId));
        }
    }

    @Transactional
    public void unlink(long parentId, long studentId) {
        users.lockByAccountId(parentId).ifPresent(parent ->
                links.deleteByParentAccountIdAndStudentAccountId(parentId, studentId));
    }

    private static ResponseStatusException profilePending() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Account profile is not ready yet");
    }
}
