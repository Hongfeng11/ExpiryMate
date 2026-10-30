package com.expirymate.auth;

public record AuthResponse(String id, String email, String displayName) {
    public static AuthResponse from(UserPrincipal user) {
        return new AuthResponse(user.getId(), user.getEmail(), user.getDisplayName());
    }
}
