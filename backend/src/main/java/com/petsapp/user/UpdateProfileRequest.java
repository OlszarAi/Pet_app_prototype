package com.petsapp.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dane do aktualizacji profilu. Oba pola sa opcjonalne — null oznacza brak zmiany.
 *
 * <p>Username musi zawierac tylko litery, cyfry i podkreslenie (bez spacji, kreski itp.) — ta sama
 * zasada co przy rejestracji.
 */
public record UpdateProfileRequest(
    @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
        @Pattern(
            regexp = "^[a-zA-Z0-9_]*$",
            message = "Username may only contain letters, digits and underscores")
        String username,
    @Size(max = 150, message = "Bio must not exceed 150 characters") String bio) {}
