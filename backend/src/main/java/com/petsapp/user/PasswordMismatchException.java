package com.petsapp.user;

/**
 * Rzucany gdy uzytkownik podaje nieprawidlowe aktualne haslo przy zmianie hasla.
 *
 * <p>Mapowany przez GlobalExceptionHandler na HTTP 400 Bad Request — nie ujawniamy czy haslo
 * istnieje (nie zwracamy 401, ktory sugerowałby, ze konto ma inne haslo).
 */
public class PasswordMismatchException extends RuntimeException {

  public PasswordMismatchException(String message) {
    super(message);
  }
}
