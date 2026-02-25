package com.petsapp.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Odpowiada za generowanie i walidacje access tokenow JWT.
 *
 * <p>
 * Access token zawiera userId i email jako claims — pozwala to uniknac
 * zapytania do bazy przy
 * kazdym zadaniu. Refresh token to losowy UUID przechowywany w bazie jako hash
 * SHA-256.
 */
@Component
public class JwtProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtProvider.class);
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_EMAIL = "email";

    private final JwtProperties jwtProperties;

    public JwtProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * Generuje access token JWT dla uzytkownika.
     *
     * @param user uzytkownik dla ktorego generujemy token
     * @return podpisany JWT jako String
     */
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtProperties.getAccessTokenExpirationMs());

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_USER_ID, user.getId().toString())
                .claim(CLAIM_EMAIL, user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Generuje losowy refresh token (UUID) i zwraca jego hash SHA-256 do zapisu w
     * bazie.
     *
     * @return rekord zawierajacy raw token (dla klienta) i hash (do bazy)
     */
    public RefreshTokenPair generateRefreshToken() {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawToken);
        return new RefreshTokenPair(rawToken, tokenHash);
    }

    /**
     * Waliduje access token i zwraca claims.
     *
     * @param token JWT jako String
     * @return Optional z claims jesli token jest wazny
     */
    public Claims validateAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Sprawdza czy token jest syntaktycznie poprawny i wazny — bez rzucania
     * wyjatkow.
     *
     * @param token JWT jako String
     * @return true jesli token jest wazny
     */
    public boolean isTokenValid(String token) {
        try {
            validateAccessToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Wyciaga userId z access tokena bez pelnej walidacji (uzywaj po isTokenValid).
     *
     * @param token JWT jako String
     * @return UUID uzytkownika
     */
    public UUID extractUserId(String token) {
        Claims claims = validateAccessToken(token);
        return UUID.fromString(claims.get(CLAIM_USER_ID, String.class));
    }

    /**
     * Hashuje raw token SHA-256 do zapisu/porownania w bazie.
     *
     * @param rawToken surowy token (UUID string)
     * @return hex string SHA-256 hasha
     */
    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 jest zawsze dostepny w JDK — ten wyjatek moze sie nie zdarzyc
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /** Para (rawToken dla klienta, tokenHash do bazy danych). */
    public record RefreshTokenPair(String rawToken, String tokenHash) {
    }
}
