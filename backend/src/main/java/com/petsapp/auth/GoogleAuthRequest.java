package com.petsapp.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Zadanie logowania / rejestracji przez Google Sign-In.
 *
 * <p>Klient wysyla ID token otrzymany z Google Sign-In SDK. Backend weryfikuje token za pomoca
 * klucza publicznego Google, nie wymaga dodatkowych danych od klienta.
 */
public record GoogleAuthRequest(
    @NotBlank(message = "Google ID token is required") String idToken) {}
