package com.expirymate.shared;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListInvitationRepository extends JpaRepository<ListInvitation, String> {
    Optional<ListInvitation> findByTokenHash(String tokenHash);
}
