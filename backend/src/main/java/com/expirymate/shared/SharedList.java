package com.expirymate.shared;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "shared_lists")
public class SharedList {
    @Id @Column(length = 36, columnDefinition = "char(36)")
    private String id;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(name = "created_by", nullable = false, length = 36, columnDefinition = "char(36)")
    private String createdBy;
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected SharedList() {}
    public SharedList(String id, String name, String createdBy) {
        this.id = id;
        this.name = name;
        this.createdBy = createdBy;
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getCreatedBy() { return createdBy; }
}
