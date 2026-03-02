package com.petsapp.auth;

/**
 * Zweryfikowane dane uzytkownika z tokena Google ID.
 *
 * <p>Uzywany wewnetrznie przez OAuthService — nie jest eksponowany przez API. Wartosc name moze byc
 * null gdy Google nie udostepnia imienia.
 */
public record GoogleIdPayload(String subject, String email, String name) {}
