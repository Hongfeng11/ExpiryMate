package com.expirymate.item;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, String> {
    Page<Item> findAllByUserIdAndDeletedAtIsNull(String userId, Pageable pageable);
    Page<Item> findAllByUserIdAndDeletedAtIsNullAndStatus(String userId, ItemStatus status, Pageable pageable);
    Page<Item> findAllByUserIdAndDeletedAtIsNullAndCategory(String userId, String category, Pageable pageable);
    Optional<Item> findByIdAndUserIdAndDeletedAtIsNull(String id, String userId);
}
