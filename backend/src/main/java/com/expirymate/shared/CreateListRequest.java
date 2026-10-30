package com.expirymate.shared;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateListRequest(@NotBlank @Size(max = 100) String name) {}
