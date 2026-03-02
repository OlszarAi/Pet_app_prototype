package com.petsapp.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Rzucany gdy zasob juz istnieje (np. email lub username zajety). */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {

  public ConflictException(String message) {
    super(message);
  }
}
