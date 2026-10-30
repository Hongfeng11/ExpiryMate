package com.expirymate.shared;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "shared_list_members")
public class SharedListMember {
    @Id @Column(length = 36, columnDefinition = "char(36)")
    private String id;
    @Column(name = "list_id", nullable = false, length = 36, columnDefinition = "char(36)")
    private String listId;
    @Column(name = "user_id", nullable = false, length = 36, columnDefinition = "char(36)")
    private String userId;
    @Column(nullable = false, length = 20)
    private String role;
    @Column(name = "joined_at", insertable = false, updatable = false)
    private Instant joinedAt;

    protected SharedListMember() {}
    public SharedListMember(String id, String listId, String userId, String role) {
        this.id = id;
        this.listId = listId;
        this.userId = userId;
        this.role = role;
    }
    public String getListId() { return listId; }
    public String getUserId() { return userId; }
    public String getRole() { return role; }
}
