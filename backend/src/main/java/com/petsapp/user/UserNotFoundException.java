package com.petsapp.user;

/**
 * Wyjątek rzucany gdy uzytkownik o podanym ID lub username nie zostal znaleziony.
 *
 * <p>Mapowany przez GlobalExceptionHandler na 404 Not Found.
 */
public class UserNotFoundException extends RuntimeException {

  public UserNotFoundException(String message) {
    super(message);
  }
}
