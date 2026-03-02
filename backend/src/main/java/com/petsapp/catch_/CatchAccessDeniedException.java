package com.petsapp.catch_;

import java.util.UUID;

/**
 * Rzucany gdy uzytkownik probuje zmodyfikowac lub usunac cudzego catcha.
 *
 * <p>Mapowany na HTTP 403 przez GlobalExceptionHandler.
 */
public class CatchAccessDeniedException extends RuntimeException {

  public CatchAccessDeniedException(UUID catchId) {
    super("Access denied to catch: " + catchId);
  }
}
