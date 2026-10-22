package com.expirymate.item;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "items")
public class Item {
    @Id @Column(length = 36)
    private String id;
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, length = 60)
    private String category = "其他";
    @Column(length = 120)
    private String location;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity = BigDecimal.ONE;
    @Column(nullable = false, length = 30)
    private String unit = "件";
    @Column(name = "manufactured_on")
    private LocalDate manufacturedOn;
    @Column(name = "purchased_on")
    private LocalDate purchasedOn;
    @Column(name = "shelf_life_value")
    private Integer shelfLifeValue;
    @Enumerated(EnumType.STRING) @Column(name = "shelf_life_unit", length = 10)
    private ShelfLifeUnit shelfLifeUnit;
    @Column(name = "expires_on", nullable = false)
    private LocalDate expiresOn;
    @Column(name = "opened_on")
    private LocalDate openedOn;
    @Column(columnDefinition = "text")
    private String notes;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ItemStatus status = ItemStatus.active;
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;
    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Item() {}

    public static Item create(String id, String userId, ItemRequest request) {
        Item item = new Item();
        item.id = id;
        item.userId = userId;
        item.apply(request, false);
        return item;
    }

    public void apply(ItemRequest request, boolean patch) {
        if (!patch || request.name() != null) this.name = request.name();
        if (!patch || request.category() != null) this.category = request.category() == null ? "其他" : request.category();
        if (!patch || request.location() != null) this.location = request.location();
        if (!patch || request.quantity() != null) this.quantity = request.quantity() == null ? BigDecimal.ONE : request.quantity();
        if (!patch || request.unit() != null) this.unit = request.unit() == null ? "件" : request.unit();
        if (!patch || request.manufacturedOn() != null) this.manufacturedOn = request.manufacturedOn();
        if (!patch || request.purchasedOn() != null) this.purchasedOn = request.purchasedOn();
        if (!patch || request.shelfLifeValue() != null) this.shelfLifeValue = request.shelfLifeValue();
        if (!patch || request.shelfLifeUnit() != null) this.shelfLifeUnit = request.shelfLifeUnit();
        if (!patch || request.expiresOn() != null) this.expiresOn = request.expiresOn();
        if (!patch || request.openedOn() != null) this.openedOn = request.openedOn();
        if (!patch || request.notes() != null) this.notes = request.notes();
        if (request.status() != null) this.status = request.status();
    }

    public void softDelete() { this.deletedAt = Instant.now(); }
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getLocation() { return location; }
    public BigDecimal getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public LocalDate getManufacturedOn() { return manufacturedOn; }
    public LocalDate getPurchasedOn() { return purchasedOn; }
    public Integer getShelfLifeValue() { return shelfLifeValue; }
    public ShelfLifeUnit getShelfLifeUnit() { return shelfLifeUnit; }
    public LocalDate getExpiresOn() { return expiresOn; }
    public LocalDate getOpenedOn() { return openedOn; }
    public String getNotes() { return notes; }
    public ItemStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
