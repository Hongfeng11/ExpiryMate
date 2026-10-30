package com.expirymate.shared;

import java.time.Instant;

public record InvitationResponse(String token, String role, Instant expiresAt) {}
