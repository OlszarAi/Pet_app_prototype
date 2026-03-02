package com.petsapp.catch_;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;

/**
 * Serwis przetwarzania obrazow dla catchy psow.
 *
 * <p>Wykonuje trzy operacje na kazdym wyslanym zdjeciu:
 * <ol>
 *   <li>Zmiana rozmiaru do max {@value #FULL_WIDTH}px (pelna wersja).
 *   <li>Zmiana rozmiaru do {@value #THUMB_WIDTH}px (miniatura dla feedu).
 *   <li>Konwersja do JPEG i stripping metadanych EXIF (Thumbnailator usuwa EXIF automatycznie).
 * </ol>
 *
 * <p>Obraz jest przechowywany w pamieci (ByteArrayOutputStream) — dla max 10MB jest to bezpieczne.
 * Produkcyjne skalowanie (>100 req/s) wymaga zewnetrznego serwisu przetwarzania obrazow.
 */
@Component
public class ImageResizer {

  static final int FULL_WIDTH = 800;
  static final int THUMB_WIDTH = 200;
  static final String OUTPUT_FORMAT = "jpeg";
  static final float OUTPUT_QUALITY = 0.85f;
  static final String CONTENT_TYPE = "image/jpeg";

  /**
   * Przetwarza przeslany obraz do pelnego rozmiaru ({@value #FULL_WIDTH}px).
   *
   * @param inputStream strumien wejsciowy oryginalnego pliku
   * @return bajty przetworzonego obrazu JPEG
   * @throws ImageProcessingException gdy plik jest uszkodzony lub nieobslugiwany
   */
  public ProcessedImage resizeFull(InputStream inputStream) {
    return resize(inputStream, FULL_WIDTH);
  }

  /**
   * Przetwarza przeslany obraz do miniatury ({@value #THUMB_WIDTH}px).
   *
   * @param inputStream strumien wejsciowy oryginalnego pliku
   * @return bajty przetworzonego obrazu JPEG
   * @throws ImageProcessingException gdy plik jest uszkodzony lub nieobslugiwany
   */
  public ProcessedImage resizeThumbnail(InputStream inputStream) {
    return resize(inputStream, THUMB_WIDTH);
  }

  private ProcessedImage resize(InputStream inputStream, int maxWidth) {
    try {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      Thumbnails.of(inputStream)
          .width(maxWidth)
          .outputFormat(OUTPUT_FORMAT)
          .outputQuality(OUTPUT_QUALITY)
          .toOutputStream(outputStream);

      byte[] bytes = outputStream.toByteArray();
      return new ProcessedImage(new ByteArrayInputStream(bytes), bytes.length, CONTENT_TYPE);
    } catch (IOException e) {
      throw new ImageProcessingException("Could not process uploaded image", e);
    }
  }

  /**
   * Wynik przetwarzania obrazu — gotowy do przekazania do StorageService.upload().
   *
   * @param inputStream strumien danych przetworzonego obrazu
   * @param contentLength rozmiar w bajtach
   * @param contentType MIME type (zawsze image/jpeg)
   */
  public record ProcessedImage(InputStream inputStream, long contentLength, String contentType) {}
}
