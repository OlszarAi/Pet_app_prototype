package com.petsapp.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Zadanie ustawienia nowego hasla z uzyciem jednorazowego tokena.
 *
 * <p>Token jest raw UUID wyslanym w linku emailowym. Serwis hashuje go przed porownaniem z baza.
 */
public record ResetPasswordRequest(
    @NotBlank(message = "Token is required") String token,
    @NotBlank(message = "New password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        String newPassword) {}
