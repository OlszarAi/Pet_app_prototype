package com.petsapp.common;

/**
 * Kody bledow zwracane w ApiResponse.ErrorDetail.code.
 * Kazdy kod mapuje sie na konkretny HTTP status w GlobalExceptionHandler.
 */
public enum ErrorCode {
    VALIDATION_ERROR,    // 400 — bledy walidacji DTO
    UNAUTHORIZED,        // 401 — brak lub nieprawidlowy token
    FORBIDDEN,           // 403 — brak uprawnien do zasobu
    NOT_FOUND,           // 404 — zasob nie istnieje
    CONFLICT,            // 409 — duplikat (username, email, like)
    RATE_LIMITED,        // 429 — za duzo requestow
    FILE_TOO_LARGE,      // 413 — zdjecie > 10MB
    UNSUPPORTED_FORMAT,  // 415 — nieprawidlowy format pliku
    INTERNAL_ERROR       // 500 — nieoczekiwany blad serwera
}
