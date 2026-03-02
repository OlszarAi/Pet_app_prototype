package com.petsapp.storage;

import java.io.InputStream;

/**
 * Kontrakt dla serwisu przechowywania plikow.
 *
 * <p>Umozliwia podmiane implementacji (S3, MinIO, lokalny disk) bez zmian w kodzie biznesowym.
 * Implementacja produkcyjna: S3StorageService. Implementacja testowa: mockowana przez Mockito.
 */
public interface StorageService {

  /**
   * Wgrywa plik do bucketu i zwraca publiczny URL.
   *
   * @param key sciezka klucza w buckecie, np. "avatars/uuid.jpg"
   * @param inputStream strumien danych pliku
   * @param contentType MIME type przesylanego pliku
   * @param contentLength dlugosc treści w bajtach
   * @return publiczny URL do pobranego pliku
   */
  String upload(String key, InputStream inputStream, String contentType, long contentLength);

  /**
   * Usuwa plik z bucketu.
   *
   * @param key sciezka klucza w buckecie
   */
  void delete(String key);
}
