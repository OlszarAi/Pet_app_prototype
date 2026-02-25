package com.petsapp.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Przekazywany przez klienta przy wylogowaniu — identyfikuje konkretny refresh
 * token do usuniecia.
 */
public record LogoutRequest(
        @NotBlank(message = "Refresh token is required") String refreshToken) {
}
