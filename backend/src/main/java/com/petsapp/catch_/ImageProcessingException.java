package com.petsapp.catch_;

/**
 * Rzucany gdy przetwarzanie obrazu (resize, strip EXIF) zakonczylo sie niepowodzeniem.
 *
 * <p>Mapowany na HTTP 400 przez GlobalExceptionHandler (uszkodzony lub nieobslugiwany plik).
 */
public class ImageProcessingException extends RuntimeException {

  public ImageProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}
