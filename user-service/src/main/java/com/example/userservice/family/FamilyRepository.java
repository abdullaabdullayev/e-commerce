package com.example.userservice.family;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

interface FamilyRepository extends JpaRepository<ParentStudentLink, Long> {
    boolean existsByParentAccountIdAndStudentAccountId(long parentId, long studentId);
    void deleteByParentAccountIdAndStudentAccountId(long parentId, long studentId);

    @Query("""
        select new com.example.userservice.family.ChildProfile(u.accountId, u.firstName, u.lastName)
        from ParentStudentLink link join User u on u.accountId = link.studentAccountId
        where link.parentAccountId = :parentId order by u.firstName, u.lastName, u.accountId
        """)
    List<ChildProfile> children(long parentId);
}
