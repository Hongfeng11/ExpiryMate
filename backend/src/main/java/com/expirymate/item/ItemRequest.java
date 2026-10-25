package com.expirymate.item;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ItemRequest(
        @Size(max = 120) String name,
        @Size(max = 60) String category,
        @Size(max = 120) String location,
        @DecimalMin(value = "0.00") BigDecimal quantity,
        @Size(max = 30) String unit,
        LocalDate manufacturedOn,
        LocalDate purchasedOn,
        @Positive Integer shelfLifeValue,
        ShelfLifeUnit shelfLifeUnit,
        LocalDate expiresOn,
        LocalDate openedOn,
        @Size(max = 5000) String notes,
        ItemStatus status,
        LocalDate packageExpiresOn,
        @Positive Integer openedShelfLifeValue,
        ShelfLifeUnit openedShelfLifeUnit,
        Boolean clearPackageExpiresOn,
        Boolean clearOpenedOn,
        Boolean clearShelfLife,
        Boolean clearOpenedShelfLife
) {}
