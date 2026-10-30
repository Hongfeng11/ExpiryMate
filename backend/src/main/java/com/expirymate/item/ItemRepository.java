package com.expirymate.item;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, String> {
    @Query("""
            SELECT i FROM Item i
            WHERE i.listId = :listId AND i.deletedAt IS NULL
              AND (:status IS NULL OR i.status = :status)
              AND (:category IS NULL OR i.category = :category)
              AND (:location IS NULL OR LOWER(COALESCE(i.location, '')) = LOWER(:location))
              AND (:keyword IS NULL
                   OR LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(i.category) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(COALESCE(i.location, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(COALESCE(i.notes, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Item> searchItems(@Param("listId") String listId,
                           @Param("status") ItemStatus status,
                           @Param("category") String category,
                           @Param("location") String location,
                           @Param("keyword") String keyword,
                           Pageable pageable);
    Optional<Item> findByIdAndListIdAndDeletedAtIsNull(String id, String listId);
}
