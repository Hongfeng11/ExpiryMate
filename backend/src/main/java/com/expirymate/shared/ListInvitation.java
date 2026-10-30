package com.expirymate.shared;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "list_invitations")
public class ListInvitation {
    @Id @Column(length = 36, columnDefinition = "char(36)")
    private String id;
    @Column(name = "list_id", nullable = false, length = 36, columnDefinition = "char(36)")
    private String listId;
    @Column(name = "token_hash", nullable = false, length = 64, columnDefinition = "char(64)")
    private String tokenHash;
    @Column(nullable = false, length = 20)
    private String role;
    @Column(name = "created_by", nullable = false, length = 36, columnDefinition = "char(36)")
    private String createdBy;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "accepted_by", length = 36, columnDefinition = "char(36)")
    private String acceptedBy;
    @Column(name = "accepted_at")
    private Instant acceptedAt;

    protected ListInvitation() {}
    public ListInvitation(String id, String listId, String tokenHash, String role,
                          String createdBy, Instant expiresAt) {
        this.id = id;
        this.listId = listId;
        this.tokenHash = tokenHash;
        this.role = role;
        this.createdBy = createdBy;
        this.expiresAt = expiresAt;
    }
    public String getListId() { return listId; }
    public String getRole() { return role; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getAcceptedBy() { return acceptedBy; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public void accept(String userId) { this.acceptedBy = userId; this.acceptedAt = Instant.now(); }
}
