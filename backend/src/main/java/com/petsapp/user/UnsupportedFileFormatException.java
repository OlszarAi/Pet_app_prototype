package com.petsapp.user;

/**
 * Rzucany gdy uzytkownik wgrywa plik w niedozwolonym formacie.
 *
 * <p>Mapowany przez GlobalExceptionHandler na HTTP 415 Unsupported Media Type.
 */
public class UnsupportedFileFormatException extends RuntimeException {

  public UnsupportedFileFormatException(String message) {
    super(message);
  }
}
