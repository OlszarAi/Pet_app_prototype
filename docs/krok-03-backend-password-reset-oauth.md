# Krok 3 — Backend: Reset hasla + OAuth Google

## Co zostalo zrobione

Krok 3 dodaje dwa niezalezne scenariusze uwierzytelniania na bazie istniejacego systemu z Kroku 2:

1. **Reset hasla przez email** — uzytkownik podaje email, dostaje link z tokenem, ustawia nowe haslo.
2. **Logowanie przez Google** — uzytkownik podaje `idToken` z Google SDK, backend weryfikuje go
   offline przez Google API i zwraca pare tokenow JWT tak samo jak przy logowaniu emailem.

Po Kroku 3 liczba testow: **25 (12 z Kroku 2 + 7 PasswordResetIntegration + 6 OAuthService unit)**.

---

## Struktura plikow po Kroku 3

Nowe pliki sa oznaczone `<- NOWY`, zmodyfikowane `<- ZAKTUALIZOWANY`.

```
backend/
└── src/
    ├── main/
    │   ├── java/com/petsapp/
    │   │   └── auth/
    │   │       ├── PasswordReset.java                  <- NOWY: encja JPA tabeli password_reset
    │   │       ├── PasswordResetRepository.java        <- NOWY: repo (findByTokenHash, deleteAllByUserId)
    │   │       ├── ForgotPasswordRequest.java          <- NOWY: DTO { email }
    │   │       ├── ResetPasswordRequest.java           <- NOWY: DTO { token, newPassword }
    │   │       ├── GoogleAuthRequest.java              <- NOWY: DTO { idToken }
    │   │       ├── GoogleIdPayload.java                <- NOWY: record { subject, email, name }
    │   │       ├── GoogleTokenVerifier.java            <- NOWY: interfejs (umozliwia mock w testach)
    │   │       ├── GoogleTokenVerifierImpl.java        <- NOWY: impl produkcyjna @Profile("!test")
    │   │       ├── OAuthService.java                   <- NOWY: logika find-or-create dla Google
    │   │       ├── AuthService.java                    <- ZAKTUALIZOWANY: forgotPassword, resetPassword, googleLogin
    │   │       ├── AuthController.java                 <- ZAKTUALIZOWANY: 3 nowe endpointy
    │   │       ├── EmailService.java                   <- ZAKTUALIZOWANY: sendPasswordResetEmail
    │   │       ├── RateLimitService.java               <- ZAKTUALIZOWANY: checkForgotPasswordRateLimit
    │   │       ├── User.java                           <- ZAKTUALIZOWANY: linkOAuth()
    │   │       └── UserRepository.java                 <- ZAKTUALIZOWANY: findByOauthProviderAndOauthId
    │   └── resources/
    │       ├── application.yml                         <- ZAKTUALIZOWANY: google.client-id
    │       └── application-dev.yml                     <- ZAKTUALIZOWANY: google.client-id
    └── test/
        └── java/com/petsapp/
            └── auth/
                ├── PasswordResetIntegrationTest.java   <- NOWY: 7 testow integracyjnych
                ├── OAuthServiceTest.java               <- NOWY: 6 testow jednostkowych
                └── TestGoogleTokenVerifier.java        <- NOWY: stub @Profile("test")
```

---

## Opis kazdego elementu

### Encja PasswordReset

Mapuje tabele `password_reset` z migracji `V1__init_schema.sql` (tabela juz istniala).

```java
@Entity @Table(name = "password_reset")
public class PasswordReset {
    UUID id;
    @ManyToOne(LAZY) User user;
    String tokenHash;        // SHA-256 hash tokenu (raw nigdy nie trafia do bazy)
    LocalDateTime expiresAt; // teraz + 1 godzina
    boolean used;            // czy token juz zostal zuzyty
    LocalDateTime createdAt;

    boolean isExpired()  { return LocalDateTime.now().isAfter(expiresAt); }
    boolean isUsed()     { return used; }
    void markUsed()      { this.used = true; }
}
```

Dlaczego hash zamiast raw tokenu? Identyczna zasada co w `RefreshToken` — jesli baza wycieknie,
atakujacy nie dostaje dzialajacych tokenow resetowania.

### Interfejs GoogleTokenVerifier i implementacja

`GoogleTokenVerifier` to interfejs z jedna metoda:

```java
GoogleIdPayload verify(String idToken) throws AuthException;
```

Dwie implementacje:

| Klasa | Profil | Zachowanie |
|---|---|---|
| `GoogleTokenVerifierImpl` | `!test` (produkcja + dev) | Weryfikuje przez Google API (`GoogleIdTokenVerifier`) |
| `TestGoogleTokenVerifier` | `test` | Stub — akceptuje `"valid-google-token"`, rzuca AuthException dla reszty |

Dlaczego interfejs? Bez niego testy integracyjne wymagalyby prawdziwego `GOOGLE_CLIENT_ID` i
dostepu do internetu. Interfejs pozwala zamienic implementacje przez Spring profiles bez zmiany
zadnego kodu produkcyjnego.

**Wazna poprawka w GoogleTokenVerifierImpl** — biblioteka google-api-client rzuca
`IllegalArgumentException` (unchecked) dla zdeformowanych tokenow base64, nie `GeneralSecurityException`.
Bez dodatkowego `catch (Exception e)` serwer zwracalby `INTERNAL_ERROR` zamiast `UNAUTHORIZED`.
Poprawka:

```java
} catch (AuthException e) {
    throw e;
} catch (GeneralSecurityException | IOException e) {
    log.error("Google ID token verification failed: {}", e.getMessage());
    throw new AuthException("Google authentication failed. Please try again.");
} catch (Exception e) {
    // Google library rzuca IllegalArgumentException dla malformed base64 tokenow
    log.warn("Google ID token rejected (malformed or invalid): {}", e.getMessage());
    throw new AuthException("Invalid Google ID token.");
}
```

### OAuthService — logika find-or-create

`authenticateWithGoogle(idToken)` to jedyna metoda publiczna. Wewnetrznie `findOrCreateUser(payload)`
obsluguje trzy przypadki:

```
Case 1: findByOauthProviderAndOauthId("google", subject)
    -> konto Google juz polaaczone z kontem w aplikacji
    -> zwroc usera wprost, bez zmian

Case 2: findActiveByEmail(payload.email())
    -> konto z tym emailem juz istnieje (zalozono przez email/haslo)
    -> wywolaj user.linkOAuth("google", subject)
    -> zapisz — konto jest teraz polaczone z Google

Case 3: brak konta
    -> utworz nowego uzytkownika:
       - username: normalizuj Google display name (NFD->ASCII->lowercase->_)
       - skroc do 20 znakow
       - jesli kolizja: dodaj sufiks UUID 6 znakow (do 10 prob)
       - haslo: null (uzytkownik nie ma hasla, moze je ustawic pozniej)
       - markEmailVerified() = true (Google juz potwierdzil email)
```

Normalizacja username przykladowo:
- `"Marta Kowalska"` → `"marta_kowalska"`
- `"André Ó'Brien"` → `"andre_o_brien"`
- Zbyt dlugi → skroc do 20 znakow
- Kolizja w bazie → `"marta_kowalska_a3f9b2"`

### AuthService — nowe metody

**`forgotPassword(ForgotPasswordRequest)`**:
1. Szuka uzytkownika po emailu — jesli nie istnieje, **zwraca sukces bez bledu** (ochrona przed
   wyliczaniem uzytkownikow — atakujacy nie wie czy email jest zarejestrowany)
2. Usuwa wszystkie stare tokeny resetowania dla tego uzytkownika (`deleteAllByUserId`)
3. Generuje nowy token (UUID v4 przez `SecureRandom`), liczy SHA-256 hash, zapisuje w bazie (TTL 1h)
4. Asynchronicznie wysyla email z linkiem `{frontendUrl}/reset-password?token={rawToken}`

**`resetPassword(ResetPasswordRequest)`**:
1. SHA-256 hash podanego tokenu → szuka w bazie (`findByTokenHash`)
2. Walidacja: token musi istniec + nie przeterminowany + nie uzyty
3. BCrypt(12) nowego hasla
4. `passwordReset.markUsed()` (token jednorazowy)
5. **`refreshTokenRepository.deleteAllByUserId(userId)`** — uniewa wszystkie aktywne sesje
   (jesli atakujacy przejal sesje, po resecie hasla jego tokeny przestaja dzialac)

**`googleLogin(GoogleAuthRequest)`**:
1. Deleguje weryfikacje tokenu do `OAuthService.authenticateWithGoogle(idToken)`
2. Wywoluje `generateAndPersistTokens(user)` — ten sam mechanizm co przy normalnym logowaniu
3. Zwraca `AuthResponse` z parami tokenow JWT

### EmailService — sendPasswordResetEmail

```java
@Async
public void sendPasswordResetEmail(String toEmail, String resetLink, String username) {
    // Temat: "PetsApp - reset hasla"
    // Tresc: prosty tekst z linkiem resetujacym i informacja o TTL 1h
}
```

### Nowe endpointy w AuthController

| Metoda | Path | Rate limit | Opis |
|---|---|---|---|
| `POST` | `/auth/forgot-password` | 3 / minuta | Wyslij link resetujacy na email |
| `POST` | `/auth/reset-password` | brak | Zmien haslo uzywajac tokenu z emaila |
| `POST` | `/auth/google` | brak | Zaloguj sie tokenem Google ID |

Brak rate limitu na `reset-password` — token w URL jest jednorazowy i wygasa po 1h, wiec
brute-force jest niemozliwy (32 bajty entropii z SecureRandom).
Brak rate limitu na `google` — Google token jest juz zwalidowany przez Google, musi byc swiezy.

---

## Nowe endpointy — przykladowe zadania i odpowiedzi

### POST /auth/forgot-password

```json
// Request:
{ "email": "user@example.com" }

// Response (HTTP 200) — taka sama niezaleznie od tego czy email istnieje:
{
  "success": true,
  "data": { "message": "If this email is registered, you will receive a password reset link." }
}
```

### POST /auth/reset-password

```json
// Request:
{
  "token": "fe828d4b-6e67-4949-9925-5d9e79578b39",
  "newPassword": "NewSecurePass456"
}

// Response (HTTP 200):
{ "success": true, "data": { "message": "Password has been reset successfully." } }

// Error — token przeterminowany lub zuzyty (HTTP 401):
{ "success": false, "error": { "code": "UNAUTHORIZED", "message": "Password reset token has expired." } }
{ "success": false, "error": { "code": "UNAUTHORIZED", "message": "Password reset token has already been used." } }
```

### POST /auth/google

```json
// Request:
{ "idToken": "<Google ID Token z SDK>" }

// Response (HTTP 200) — identyczny format co POST /auth/login:
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "6f8a2c11-...",
    "expiresIn": 900
  }
}

// Error — nieprawidlowy lub przeterminowany token Google (HTTP 401):
{ "success": false, "error": { "code": "UNAUTHORIZED", "message": "Invalid Google ID token." } }
```

---

## Flow resetowania hasla krok po kroku

```
POST /auth/forgot-password { email }
  └── RateLimitService.checkForgotPasswordRateLimit(ip)       // 3/min
  └── UserRepository.findActiveByEmail(email)                 // moze nie znalezc
  └── [jesli znaleziono]:
        PasswordResetRepository.deleteAllByUserId(userId)     // usun stare tokeny
        token = EmailService.generateResetToken()             // UUID v4 via SecureRandom
        hash = SHA-256(token)
        zapis PasswordReset (expires: teraz + 1h)
        @Async: EmailService.sendPasswordResetEmail(link)
  └── Zawsze: HTTP 200 z tym samym komunikatem

POST /auth/reset-password { token, newPassword }
  └── hash = SHA-256(token)
  └── PasswordResetRepository.findByTokenHash(hash)           // 404 -> AuthException
  └── passwordReset.isExpired()? -> AuthException
  └── passwordReset.isUsed()? -> AuthException
  └── user.updatePassword(BCrypt(newPassword))
  └── passwordReset.markUsed()
  └── RefreshTokenRepository.deleteAllByUserId(userId)        // uniewa wszystkie sesje!
  └── HTTP 200
```

---

## Gwarancje bezpieczenstwa

| Scenariusz | Zachowanie |
|---|---|
| Email nie istnieje w bazie | HTTP 200 z takim samym komunikatem (brak user enumeration) |
| Token wygasl (>1h) | HTTP 401 "expired" |
| Token juz uzyty | HTTP 401 "already been used" |
| Baza wyciekla | Tylko SHA-256 hashe — raw tokeny nie sa w bazie |
| Uzytkownik ma aktywne sesje po resecie | Wszystkie refresh tokeny uniewa z chwila zmiany hasla |
| Konto Googleowe z istniejacym emailem | Konto zostaje polaczone (linkOAuth), nie duplikowane |
| Zdeformowany Google ID token | HTTP 401 — IllegalArgumentException jest przechwytywany |

---

## Zaleznosc Maven (nowa)

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.google.api-client</groupId>
    <artifactId>google-api-client</artifactId>
    <version>2.6.0</version>
</dependency>
```

Sluzy wylacznie do offline weryfikacji podpisu Google ID tokenu.
Nie jest potrzebna komunikacja sieciowa — biblioteka weryfikuje token kryptograficznie.
(Pierwsze wywolanie pobiera Google JWKS certificate cache.)

---

## Konfiguracja

```yaml
# application.yml
google:
  client-id: ${GOOGLE_CLIENT_ID:}   # pusty default — testy nie wymagaja tej zmiennej

# application-dev.yml
google:
  client-id: ${GOOGLE_CLIENT_ID:your-google-client-id-here}

# .env (nie w repozytorium)
GOOGLE_CLIENT_ID=123456789-abc.apps.googleusercontent.com
```

**Uwaga**: bez prawdziwego `GOOGLE_CLIENT_ID` endpoint `POST /auth/google` zwroci blad w srodowisku dev.
Dla testow automatycznych `TestGoogleTokenVerifier` (@Profile("test")) jest uzywany zamiast implementacji
produkcyjnej — testy nie wymagaja Google Client ID.

---

## Testy

### PasswordResetIntegrationTest (7 testow, Testcontainers + PostgreSQL)

| Test | Co sprawdza |
|---|---|
| `forgotPassword_shouldSendResetEmail` | Zapisanie tokenu w bazie + wywolanie EmailService |
| `forgotPassword_shouldSilentlyIgnoreUnknownEmail` | Brak wyjatku dla nieznanego emaila |
| `resetPassword_shouldChangePassword` | Stare haslo odrzucone, nowe akceptowane po resecie |
| `resetPassword_shouldInvalidateAllSessions` | Refresh token przestaje dzialac po resecie |
| `resetPassword_shouldRejectExpiredToken` | `AuthException` dla wygaslego tokenu |
| `resetPassword_shouldRejectUsedToken` | `AuthException` przy probie uzycia tokenu drugi raz |
| `resetPassword_shouldRejectInvalidToken` | `AuthException` dla losowego stringa jako token |

### OAuthServiceTest (6 testow, Mockito — bez bazy)

| Test | Co sprawdza |
|---|---|
| `authenticateWithGoogle_existingOauthUser_returnsUser` | Case 1: user juz ma polaczone konto Google |
| `authenticateWithGoogle_existingEmailUser_linksAndReturns` | Case 2: linkOAuth() wywolane dla istniejacego konta |
| `authenticateWithGoogle_newUser_createsAndReturns` | Case 3: nowy uzytkownik tworzony z Google profilu |
| `authenticateWithGoogle_invalidToken_throwsAuthException` | Nieprawidlowy token → propagacja AuthException |
| `generateUniqueUsername_collision_appendsSuffix` | Kolizja username dodaje sufiks UUID |
| `authenticateWithGoogle_emailAlreadyLinkedToOAuth` | Ten sam email juz ma inne konto OAuth |

---

## Weryfikacja dzialania (live curl)

Przeprowadzona recznie po uruchomieniu serwisu (`SPRING_PROFILES_ACTIVE=dev`):

```bash
# Export zmiennych srodowiskowych z .env:
export $(cat .env | grep -v "^#" | xargs)

# Uruchom backend:
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# --- Reset hasla ---

# 1. Wyslij link resetujacy:
curl -X POST http://localhost:8080/api/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com"}'
# -> {"success":true,"data":{"message":"If this email is registered..."}}

# 2. Pobierz token z MailHog API:
curl -s http://localhost:8025/api/v2/messages | python3 -c "
import sys, json, quopri
data = json.load(sys.stdin)
body = data['items'][0]['Content']['Body']
print(quopri.decodestring(body.encode()).decode())
" | grep -oP '(?<=token=)[a-f0-9-]+'
# -> fe828d4b-6e67-4949-9925-5d9e79578b39

# 3. Ustaw nowe haslo:
curl -X POST http://localhost:8080/api/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{"token":"fe828d4b-6e67-4949-9925-5d9e79578b39","newPassword":"NewPass999"}'
# -> {"success":true,"data":{"message":"Password has been reset successfully."}}

# 4. Stare haslo odrzucone (HTTP 401), nowe akceptowane (HTTP 200)
# 5. Stary refresh token odrzucony (HTTP 401)
# 6. Token uzyty drugi raz odrzucony (HTTP 401 "already been used")

# --- Google OAuth ---

# Nieprawidlowy token -> HTTP 401 (nie INTERNAL_ERROR):
curl -X POST http://localhost:8080/api/v1/auth/google \
  -H "Content-Type: application/json" \
  -d '{"idToken":"invalid.google.token"}'
# -> {"success":false,"error":{"code":"UNAUTHORIZED","message":"Invalid Google ID token."}}

# Prawdziwy token z Google SDK -> HTTP 200 z accessToken + refreshToken
```

```
Tests run: 25, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
```

---

## Co NIE zostalo zrobione w Kroku 3 (celowo)

- Brak rotacji tokenow resetowania (link moze byc klikniety tylko raz — wystarczajace dla MVP)
- Brak linku resetujacego przez SMS — email wystarczy
- Brak Apple Sign-In — tylko Google OAuth w tej iteracji
- Brak rate limitera na `reset-password` — token jest jednorazowy i ma duza entropie (UUID v4)
- Brak frontendu do resetu hasla — endpoint `frontendUrl/reset-password?token=...` bedzie zaimplementowany w Kroku 9+

---

## Nastepny krok: Krok 4 — Profil uzytkownika + Ustawienia + Avatar

- `UserController` z endpointami: `GET /users/me`, `PATCH /users/me`, `POST /users/me/avatar`,
  `DELETE /users/me/avatar`, `PATCH /users/me/password`, `DELETE /users/me`
- `UserSettings` — `GET` + `PATCH /users/me/settings`
- `S3StorageService` — upload/delete awatara, MinIO w dev (localhost:9000)
- Nowy pakiet `user/` (nie dodajemy do `auth/`)
