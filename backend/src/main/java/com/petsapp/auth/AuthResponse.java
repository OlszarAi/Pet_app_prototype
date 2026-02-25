package com.petsapp.auth;

/**
 * Odpowiedz zawierajaca tokeny wystawione po pomyslnym logowaniu lub
 * weryfikacji emaila.
 *
 * @param accessToken  JWT access token (krotkozyciowy, 15 min)
 * @param refreshToken losowy token do odswiezania sesji (7 dni)
 * @param expiresIn    liczba sekund do wygasniecia access tokena
 */
public record AuthResponse(String accessToken, String refreshToken, long expiresIn) {
}
