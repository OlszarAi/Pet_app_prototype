package com.petsapp.auth;

/**
 * Abstrakcja weryfikacji Google ID tokenow.
 *
 * <p>Dzieki interfejsowi mozemy latwo podmieniac implementacje w testach (Mockito mock zamiast
 * prawdziwego klienta Google). Implementacja produkcyjna uzywa biblioteki google-api-client do
 * weryfikacji podpisu tokena.
 */
public interface GoogleTokenVerifier {

  /**
   * Weryfikuje Google ID token i zwraca payload.
   *
   * @param idToken raw ID token z Google Sign-In SDK (po stronie klienta)
   * @return zweryfikowane dane uzytkownika
   * @throws AuthException gdy token jest nieprawidlowy, wygasl lub pochodzi od innego klienta
   */
  GoogleIdPayload verify(String idToken);
}
