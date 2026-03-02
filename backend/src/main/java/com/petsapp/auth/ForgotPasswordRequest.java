package com.petsapp.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Zadanie inicjalizacji procesu resetu hasla.
 *
 * <p>Endpoint celowo nie informuje czy podany email istnieje w systemie (ochrona przed
 * enumeration).
 */
public record ForgotPasswordRequest(
    @NotBlank(message = "Email is required") @Email(message = "Email must be a valid address")
        String email) {}
