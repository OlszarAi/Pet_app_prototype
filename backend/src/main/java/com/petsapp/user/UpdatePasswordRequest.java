package com.petsapp.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dane do zmiany hasla. Wymaga aktualnego hasla jako dodatkowego czynnika autoryzacji — chroni
 * przed przejęciem konta gdy token JWT zostal skradziony.
 */
public record UpdatePasswordRequest(
    @NotBlank(message = "Current password is required") String currentPassword,
    @NotBlank(message = "New password is required")
        @Size(min = 8, max = 72, message = "New password must be between 8 and 72 characters")
        String newPassword) {}
