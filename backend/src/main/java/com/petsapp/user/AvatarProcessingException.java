package com.petsapp.user;

/**
 * Rzucany gdy przetwarzanie obrazu avatara nie powiodlo sie.
 *
 * <p>Mapowany przez GlobalExceptionHandler na HTTP 400 Bad Request — plik jest prawdopodobnie
 * uszkodzony lub w nieobslugowanym wariancie formatu.
 */
public class AvatarProcessingException extends RuntimeException {

  public AvatarProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}
