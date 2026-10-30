package com.expirymate.shared;

import jakarta.validation.constraints.Pattern;

public record CreateInvitationRequest(@Pattern(regexp = "EDITOR|VIEWER") String role) {}
