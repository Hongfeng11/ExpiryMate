package com.expirymate.item;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ItemRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 60) String category,
        @Size(max = 120) String location,
        @DecimalMin(value = "0.01") BigDecimal quantity,
        @Size(max = 30) String unit,
        LocalDate manufacturedOn,
        LocalDate purchasedOn,
        @Positive Integer shelfLifeValue,
        ShelfLifeUnit shelfLifeUnit,
        LocalDate expiresOn,
        LocalDate openedOn,
        @Size(max = 5000) String notes,
        ItemStatus status
) {}
