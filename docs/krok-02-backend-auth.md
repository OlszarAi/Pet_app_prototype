# Krok 2 — Backend: Auth (email + JWT)

## Co zostalo zrobione

Krok 2 to pelny system uwierzytelniania — rejestracja z weryfikacja emaila, logowanie przez JWT,
odswiezanie sesji i wylogowanie. Zero logiki biznesowej zwiazanej z psami czy feedem.
Cel: uzytkownik moze sie zarejestrowac, zweryfikowac konto i dostac tokeny sesji.

---

## Struktura plikow po Kroku 2

```
backend/
└── src/
    ├── main/
    │   ├── java/com/petsapp/
    │   │   ├── auth/
    │   │   │   ├── User.java                         <- encja JPA tabeli "user"
    │   │   │   ├── RefreshToken.java                 <- encja JPA refresh tokenow
    │   │   │   ├── EmailVerification.java            <- encja kodu weryfikacyjnego
    │   │   │   ├── UserSettings.java                 <- encja ustawien (@MapsId z User)
    │   │   │   ├── UserRepository.java               <- JPA repo (soft-delete aware)
    │   │   │   ├── RefreshTokenRepository.java       <- JPA repo
    │   │   │   ├── EmailVerificationRepository.java  <- JPA repo
    │   │   │   ├── UserSettingsRepository.java       <- JPA repo
    │   │   │   ├── JwtProperties.java                <- @ConfigurationProperties app.jwt.*
    │   │   │   ├── JwtProvider.java                  <- generuje i waliduje JWT + refresh
    │   │   │   ├── JwtFilter.java                    <- OncePerRequestFilter (Bearer token)
    │   │   │   ├── AuthException.java                <- wyjatek 401
    │   │   │   ├── ConflictException.java            <- wyjatek 409
    │   │   │   ├── RateLimitExceededException.java   <- wyjatek 429
    │   │   │   ├── RegisterRequest.java              <- DTO wejsciowe
    │   │   │   ├── LoginRequest.java                 <- DTO wejsciowe
    │   │   │   ├── VerifyEmailRequest.java           <- DTO wejsciowe
    │   │   │   ├── ResendVerificationRequest.java    <- DTO wejsciowe
    │   │   │   ├── RefreshTokenRequest.java          <- DTO wejsciowe
    │   │   │   ├── LogoutRequest.java                <- DTO wejsciowe
    │   │   │   ├── AuthResponse.java                 <- DTO wyjsciowe (accessToken + refreshToken + expiresIn)
    │   │   │   ├── EmailService.java                 <- @Async wysylka emaili przez MailHog/SMTP
    │   │   │   ├── RateLimitService.java             <- Bucket4j in-memory rate limiting
    │   │   │   ├── AuthService.java                  <- logika biznesowa auth
    │   │   │   └── AuthController.java               <- 6 endpointow /auth/*
    │   │   └── config/
    │   │       ├── SecurityConfig.java               <- ZAKTUALIZOWANY: JwtFilter + BCrypt
    │   │       └── UserDetailsServiceImpl.java       <- wymagany przez Spring Security
    └── test/
        ├── java/com/petsapp/
        │   ├── AbstractIntegrationTest.java          <- klasa bazowa: Testcontainers + @ActiveProfiles
        │   └── auth/
        │       └── AuthIntegrationTest.java          <- 12 testow integracyjnych
        └── resources/
            └── application-test.yml                 <- profil test: wylaczony Redis, mock mail
```

---

## Opis kazdego elementu

### Encje JPA

Cztery encje mapuja istniejace tabele z migracji `V1__init_schema.sql`. Zadna nie jest dodawana
przez Hibernate (`ddl-auto: validate` — Hibernate tylko sprawdza schemat).

| Encja | Tabela | Wazne szczegoly |
|---|---|---|
| `User` | `"user"` | Builder pattern, soft delete przez `deletedAt`, `markEmailVerified()` |
| `RefreshToken` | `refresh_token` | Przechowuje SHA-256 hash tokena, nie raw token |
| `EmailVerification` | `email_verification` | Kod 6-cyfrowy, TTL 15 min, `isExpired()`, `markUsed()` |
| `UserSettings` | `user_settings` | `@MapsId` — shared PK z User (jeden-do-jednego) |

Dlaczego `@MapsId` dla `UserSettings`? Eliminuje JOIN przy kazdym odczycie ustawien —
settings maja ten sam UUID co user. Brak dodatkowej kolumny `user_id`.

Dlaczego hash refresh tokena w bazie? Jesli baza wycieknie, atakujacy nie dostaje
aktywnych refreshTokenow — tylko bezuzyteczne SHA-256 hashe.

### JWT infrastruktura

**`JwtProvider`** — trzy odpowiedzialnosci:
1. `generateAccessToken(user)` — JWT podpisany HMAC-SHA256, payload: `sub`, `userId`, `email`, `iat`, `exp`
2. `generateRefreshToken()` — `SecureRandom` UUID (raw) + SHA-256 hash; zwraca pare `{rawToken, tokenHash}`
3. `validateAndGetUserId(token)` — rzuca `AuthException` gdy token wygasl, nieprawidlowy podpis lub zly format

**`JwtFilter`** (`OncePerRequestFilter`) — przy kazdym zadaniu HTTP:
1. Czyta naglowek `Authorization: Bearer <token>`
2. Jesli token wazny → wstrzykuje `UsernamePasswordAuthenticationToken` do `SecurityContextHolder`
3. Jesli brak tokena lub nieprawidlowy → nic nie robi (Security sam zwroci 401/403)

**`JwtProperties`** (`@ConfigurationProperties("app.jwt")`) — czyta z `application.yml`:
- `secret` — klucz HMAC (min. 32 znaki, w `.env`)
- `accessTokenExpirationMs` — 15 min (900 000 ms)
- `refreshTokenExpirationMs` — 7 dni (604 800 000 ms)

### Flow rejestracji i logowania

```
POST /auth/register
  └── Sprawdz unikalnosc email + username (ConflictException jesli duplikat)
  └── BCrypt(12) hashowanie hasla
  └── Zapis User + UserSettings w jednej transakcji
  └── Generowanie kodu 6-cyfrowego (SecureRandom)
  └── Zapis EmailVerification (TTL 15 min)
  └── @Async: wyslij email z kodem (nie blokuje odpowiedzi API)
  └── HTTP 201

POST /auth/verify-email
  └── Znajdz uzytkownika po emailu
  └── Sprawdz kod: istnieje, nie uzyty, nie przeterminowany
  └── user.markEmailVerified() + verification.markUsed()
  └── Generuj accessToken + refreshToken
  └── HTTP 200 + {accessToken, refreshToken, expiresIn}

POST /auth/login
  └── Znajdz uzytkownika (ogolny blad — nie ujawniamy czy email istnieje)
  └── BCrypt.matches(haslo, hash)
  └── Sprawdz czy email zweryfikowany
  └── Generuj accessToken + refreshToken
  └── HTTP 200 + {accessToken, refreshToken, expiresIn}

POST /auth/refresh
  └── SHA-256 hash podanego refreshToken
  └── Znajdz RefreshToken w bazie po hashu
  └── Sprawdz czy nie wygasl
  └── Generuj nowy accessToken (refresh token sie nie zmienia)
  └── HTTP 200 + {accessToken, refreshToken, expiresIn}

POST /auth/logout
  └── SHA-256 hash podanego refreshToken
  └── Usun RefreshToken z bazy
  └── HTTP 200

POST /auth/resend-verification
  └── Znajdz niezweryfikowane konto
  └── Wygeneruj nowy kod, wyslij email
  └── HTTP 200
```

### EmailService

Wysyla emaile przez `JavaMailSender` (konfiguracja w `application-dev.yml` → MailHog `localhost:1025`).
Metoda `sendVerificationCode()` jest `@Async` — wykonuje sie w osobnym watku, nie blokuje odpowiedzi API.
Generowanie kodu: `SecureRandom` z `nextInt(1_000_000)` → formatowany do 6 cyfr z zerami z przodu.

### RateLimitService (Bucket4j)

Ogranicza ilosc zapytan per IP aby zapobiec brute-force i spamowi emailowemu. Trzeba in-memory
(`ConcurrentHashMap<IP, Bucket>`) — brak Redis w Kroku 2, wystarczajace dla jednej instancji.

| Endpoint | Limit | Okno |
|---|---|---|
| `POST /auth/login` | 5 prob | 1 minuta |
| `POST /auth/register` | 3 rejestracje | 1 minuta |
| `POST /auth/resend-verification` | 2 wysylki | 1 minuta |

Przekroczenie → `RateLimitExceededException` → `GlobalExceptionHandler` → HTTP 429.

### SecurityConfig (zaktualizowany)

Krok 2 dodal do konfiguracji z Kroku 1:
- `JwtFilter` wstrzykniety przed `UsernamePasswordAuthenticationFilter`
- `BCryptPasswordEncoder` z `cost=12` (standard produkcyjny 2024, ~300ms na hash)
- `AuthenticationManager` bean (wymagany przez Spring Security internals)
- Publiczne endpointy rozszerzone o `/auth/register`, `/auth/login`, `/auth/verify-email`, `/auth/refresh`, `/auth/resend-verification`
- `POST /auth/logout` wymaga uwierzytelnienia (refreshToken jest dostateczny)

```
AuthenticationManager
  └── UserDetailsServiceImpl.loadUserByUsername(email)
       └── UserRepository.findActiveByEmail(email)
            └── zwraca UserDetails (email + passwordHash + ROLE_USER)
```

`UserDetailsServiceImpl` nie jest uzywany bezposrednio w auth flow (nie uzywamy
`DaoAuthenticationProvider`) — jest wymagany przez Spring Security aby wstrzyknal `PasswordEncoder`.

### GlobalExceptionHandler (zaktualizowany)

Krok 2 dodal trzy nowe handlery:

| Wyjatek | HTTP | ErrorCode |
|---|---|---|
| `AuthException` | 401 Unauthorized | `UNAUTHORIZED` |
| `ConflictException` | 409 Conflict | `CONFLICT` |
| `RateLimitExceededException` | 429 Too Many Requests | `RATE_LIMITED` |

---

## Testy integracyjne

Testujemy przez `AuthService` (nie HTTP) z prawdziwa baza PostgreSQL uruchomiona przez Testcontainers.
`EmailService` jest mockowany (`@MockBean`) — kod generowania kontrolujemy recznie w testach.

Kazdy test uzywa unikatowego emaila (`UUID.randomUUID()`) — baza nie jest czyszczona miedzy testami.

```
Tests run: 12, Failures: 0, Errors: 0 — BUILD SUCCESS
```

---

## Weryfikacja zgodnosci z AI_RULES.md

| Zasada | Wynik |
|---|---|
| Kod produkcyjny, nie prototypowy | OK — brak TODO, brak stubbow |
| Jeden plik = jedna odpowiedzialnosc | OK — AuthService tylko logika, AuthController tylko routing |
| Brak magic strings/numbers | OK — limity rate limitera jako stale, kody bledow przez `ErrorCode` |
| Komentarze opisuja DLACZEGO | OK — przy BCrypt cost, SHA-256 hashowaniu, @MapsId |
| Bledy obslugiwane | OK — trzy nowe klasy wyjatkow + handlery w GlobalExceptionHandler |
| Sekrety tylko w .env | OK — JWT secret czytany przez `${JWT_SECRET}` |
| Brak TODO/FIXME | OK |
| Logger przez SLF4J | OK — `LoggerFactory.getLogger()`, brak System.out |
| Encje JPA nie eksponowane | OK — AuthController zwraca `AuthResponse` DTO, nie `User` |
| Transakcje przy modyfikacji danych | OK — `@Transactional` na metodach AuthService |

---

## Weryfikacja dzialania

```bash
# Testy automatyczne:
mvn clean verify
# Tests run: 12, Failures: 0, Errors: 0 — BUILD SUCCESS

# Serwis uruchomiony:
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run

# Rejestracja:
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","username":"testuser","password":"TestPass123"}'
# {"success":true,"data":{"message":"Registration successful..."}}

# Email z kodem w MailHog:
# http://localhost:8025 -> Subject: "PetsApp - kod weryfikacyjny: 371528"

# Weryfikacja:
curl -X POST http://localhost:8080/api/v1/auth/verify-email \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","code":"371528"}'
# {"success":true,"data":{"accessToken":"eyJ...","refreshToken":"...","expiresIn":900}}

# Login:
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"TestPass123"}'
# {"success":true,"data":{"accessToken":"eyJ...","refreshToken":"...","expiresIn":900}}

# Rate limiting (5+ prob na minute):
# Attempt 1-4: HTTP 401
# Attempt 5: HTTP 429 Too Many Requests

# Swagger UI:
# http://localhost:8080/swagger-ui.html
```

---

## Co NIE zostalo zrobione w Kroku 2 (celowo)

- Brak OAuth Google — Krok 3
- Brak resetu hasla — Krok 3
- Brak `UserController` (`GET /users/me`) — Krok 4
- Brak rotacji refresh tokenow — celowe, prostsze audytowanie sesji
- Rate limiter w Redis (multi-instance) — zostawiony jako in-memory, wystarczajacy dla Kroku 2
- Brak testow HTTP (MockMvc/WebTestClient) — testy przez AuthService wystarczajace na tym etapie

---

## Nastepny krok: Krok 3 — Reset hasla + OAuth Google

Reset hasla: `POST /auth/forgot-password` → token na email → `POST /auth/reset-password`.
OAuth: `POST /auth/google { idToken }` → weryfikacja przez Google, upsert User.
