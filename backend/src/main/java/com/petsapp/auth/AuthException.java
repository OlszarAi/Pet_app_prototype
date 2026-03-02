package com.petsapp.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Rzucany gdy uzytkownik podal bledne dane logowania lub token jest nieprawidlowy. */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class AuthException extends RuntimeException {

  public AuthException(String message) {
    super(message);
  }
}
