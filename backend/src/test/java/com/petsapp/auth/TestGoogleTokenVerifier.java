package com.petsapp.auth;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Stub GoogleTokenVerifier dla profilu testowego.
 *
 * <p>Pozwala na start Spring contextu w testach bez potrzeby dostepu do Google API. Kazdy test
 * ktory faktycznie testuje OAuth powinien nadpisac ten bean przez @MockBean GoogleTokenVerifier.
 */
@Component
@Profile("test")
public class TestGoogleTokenVerifier implements GoogleTokenVerifier {

  @Override
  public GoogleIdPayload verify(String idToken) {
    // Celowo rzuca wyjatek — testy OAuth musza uzywac @MockBean do nadpisania
    throw new AuthException("Use @MockBean GoogleTokenVerifier to override in OAuth tests.");
  }
}
