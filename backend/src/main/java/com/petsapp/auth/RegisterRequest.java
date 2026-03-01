package com.petsapp.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dane wejsciowe do rejestracji.
 *
 * <p>username: 3-30 znakow, tylko litery, cyfry i podkreslniki. password: minimum 8 znakow.
 */
public record RegisterRequest(
    @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,
    @NotBlank(message = "Username is required")
        @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
        @Pattern(
            regexp = "^[a-zA-Z0-9_]+$",
            message = "Username may only contain letters, digits and underscores")
        String username,
    @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        String password) {}
