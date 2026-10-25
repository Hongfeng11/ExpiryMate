package com.expirymate.item;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ItemResponse(
        String id, String name, String category, String location, BigDecimal quantity,
        String unit, LocalDate manufacturedOn, LocalDate purchasedOn, Integer shelfLifeValue,
        ShelfLifeUnit shelfLifeUnit, LocalDate expiresOn, LocalDate packageExpiresOn,
        LocalDate openedOn, Integer openedShelfLifeValue, ShelfLifeUnit openedShelfLifeUnit, String notes,
        ItemStatus status, Instant createdAt, Instant updatedAt
) {
    public static ItemResponse from(Item item) {
        return new ItemResponse(item.getId(), item.getName(), item.getCategory(), item.getLocation(),
                item.getQuantity(), item.getUnit(), item.getManufacturedOn(), item.getPurchasedOn(),
                item.getShelfLifeValue(), item.getShelfLifeUnit(), item.getExpiresOn(), item.getPackageExpiresOn(),
                item.getOpenedOn(), item.getOpenedShelfLifeValue(), item.getOpenedShelfLifeUnit(), item.getNotes(),
                item.getStatus(), item.getCreatedAt(), item.getUpdatedAt());
    }
}
