package com.petsapp.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Produkcyjna weryfikacja Google ID tokenow przez biblioteke google-api-client.
 *
 * <p>Weryfikuje podpis tokena offline (korzystajac z publicznych kluczy Google pobieranych przy
 * uruchomieniu), bez additional round-trip do Google za kazdym razem. Klucze sa cachowane przez
 * biblioteke. Bean jest aktywny tylko dla profili roznych od "test" — w testach uzywamy mocka.
 */
@Component
@Profile("!test")
public class GoogleTokenVerifierImpl implements GoogleTokenVerifier {

  private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifierImpl.class);

  private final GoogleIdTokenVerifier verifier;

  public GoogleTokenVerifierImpl(@Value("${google.client-id}") String clientId) {
    this.verifier =
        new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(Collections.singletonList(clientId))
            .build();
    log.info("Google ID token verifier initialized for client: {}", clientId);
  }

  @Override
  public GoogleIdPayload verify(String idToken) {
    try {
      GoogleIdToken token = verifier.verify(idToken);
      if (token == null) {
        throw new AuthException("Invalid Google ID token.");
      }
      Payload payload = token.getPayload();
      String name = (String) payload.get("name");
      return new GoogleIdPayload(payload.getSubject(), payload.getEmail(), name);
    } catch (AuthException e) {
      throw e;
    } catch (GeneralSecurityException | IOException e) {
      log.error("Google ID token verification failed: {}", e.getMessage());
      throw new AuthException("Google authentication failed. Please try again.");
    } catch (Exception e) {
      // Biblioteka Google rzuca IllegalArgumentException dla blednie sformatowanych tokenow
      // oraz inne RuntimeException w przypadku problemow z parsowaniem
      log.warn("Google ID token rejected (malformed or invalid): {}", e.getMessage());
      throw new AuthException("Invalid Google ID token.");
    }
  }
}
