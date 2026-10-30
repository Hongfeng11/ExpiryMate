package com.expirymate.shared;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SharedListMemberRepository extends JpaRepository<SharedListMember, String> {
    Optional<SharedListMember> findByListIdAndUserId(String listId, String userId);
    List<SharedListMember> findAllByUserId(String userId);
    long countByListId(String listId);
}
