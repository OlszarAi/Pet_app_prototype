# Krok 1 — Backend: Fundament (Spring Boot + Flyway + Health)

## Co zostalo zrobione

Krok 1 to wylacznie fundament — zero logiki biznesowej, zero ficzerów.
Cel: aplikacja która startuje, laczy sie z baza, migruje schemat i odpowiada na health check.

---

## Struktura plikow po Kroku 1

```
backend/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/petsapp/
    │   │   ├── PetsAppApplication.java          <- glowna klasa Spring Boot
    │   │   ├── common/
    │   │   │   ├── ApiResponse.java             <- wrapper dla wszystkich odpowiedzi
    │   │   │   ├── ErrorCode.java               <- enum kodow bledow
    │   │   │   └── GlobalExceptionHandler.java  <- centralny handler wyjatkow
    │   │   ├── config/
    │   │   │   └── SecurityConfig.java          <- minimalna konfiguracja security
    │   │   └── health/
    │   │       └── HealthController.java        <- GET /health, GET /health/ready
    │   └── resources/
    │       ├── application.yml                  <- konfiguracja bazowa (bez sekretow)
    │       ├── application-dev.yml              <- profil dev (localhost, MailHog, MinIO)
    │       └── db/migration/
    │           ├── V1__init_schema.sql          <- pelny schemat bazy (19 tabel + indeksy)
    │           └── V2__seed_breeds.sql          <- 10 przykladowych ras psow
    └── test/
        └── java/com/petsapp/                   <- (puste — testy w Kroku 2+)
```

---

## Opis kazdego pliku

### `pom.xml` — zaleznosci Maven

Pelna lista bibliotek ktore beda uzywane przez caly projekt backendowy. Kluczowe decyzje:

| Biblioteka | Wersja | Do czego |
|---|---|---|
| Spring Boot | 3.3.5 | Framework aplikacji |
| Flyway | (z Boot) | Migracje schematu bazy |
| jjwt | 0.12.6 | Generowanie i walidacja JWT tokenow |
| SpringDoc | 2.6.0 | Swagger UI (dokumentacja API) |
| Bucket4j | 8.10.1 | Rate limiting |
| Thumbnailator | 0.4.20 | Zmiana rozmiaru i konwersja zdjec |
| AWS SDK S3 | 2.28.17 | Upload zdjec na S3/MinIO |
| Testcontainers | 1.20.3 | Testy integracyjne z prawdziwa baza |
| Spotless | 2.43.0 | Formatowanie kodu (Google Java Style) |

> Dlaczego wszystko na raz? Bo POM jest trudny do pozniejszego iteracyjnego rozszerzania bez problemow z wersjami. Lepiej zadeklarowac wszystkie zaleznosci na poczatku i dodawac implementacje po kroku.

### `PetsAppApplication.java`

Glowna klasa Spring Boot z dwoma adnotacjami poza @SpringBootApplication:
- `@EnableAsync` — wymagane do wysylki push notyfikacji w tle (Krok 8)
- `@EnableScheduling` — wymagane do schedulera feedu i czyszczenia kont (Krok 7/8)

### `application.yml` — konfiguracja bazowa

Nie zawiera zadnych sekretow ani wartosci specyficznych dla srodowiska. Definiuje:
- `ddl-auto: validate` — Hibernate sprawdza schemat ale go nie modyfikuje (Flyway to robi)
- Limity uploadow: max 10MB na plik, 12MB na caly request
- Context path `/api/v1` — wszystkie endpointy maja ten prefix
- JWT expiry: access token 15 min, refresh token 7 dni

### `application-dev.yml` — profil deweloperski

Aktywowany przez `SPRING_PROFILES_ACTIVE=dev`. Wskazuje na lokalne serwisy Docker:
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`
- MailHog: `localhost:1025` (SMTP)
- MinIO: `localhost:9000` (S3-compatible)

Wszystkie hasla sa czytane ze zmiennych srodowiskowych (plik `.env`, nigdy nie commitowany).

### `ApiResponse<T>` — wrapper odpowiedzi

Kazdy endpoint zawsze zwraca ten sam format JSON:

```json
// Sukces
{"success": true, "data": {...}}

// Sukces z paginacja
{"success": true, "data": [...], "pagination": {"cursor": "abc", "hasMore": true}}

// Blad
{"success": false, "error": {"code": "VALIDATION_ERROR", "message": "...", "fields": {...}}}
```

Zaimplementowany jako Java 21 `record` — immutable, zwiezly. Zagniezdzone typy `ErrorDetail` i `PaginationMeta` sa rowniez recordami. Metody fabryczne `ok()` i `error()` sprawiaja ze kontrolery sa czytelne:

```java
return ResponseEntity.ok(ApiResponse.ok(Map.of("status", "UP")));
```

### `ErrorCode` — enum kodow bledow

9 kodow ktore odpowiadaja umowie z README. Uzywane w `GlobalExceptionHandler` i we wszystkich serwisach. Zaden kod bledu nie moze byc zwrocony jako raw string.

### `GlobalExceptionHandler` — centralny handler wyjatkow

Obsluguje 4 rodzaje wyjatkow:
1. `MethodArgumentNotValidException` — walidacja DTO (`@Valid`), zwraca mape pola -> blad
2. `ConstraintViolationException` — walidacja na poziomie serwisu (`@Validated`)
3. `MaxUploadSizeExceededException` — plik > 10MB → HTTP 413
4. `Exception` (fallback) — nieoczekiwany wyjatek, loguje stack trace, klientowi zwraca ogolny komunikat

Klient nigdy nie widzi wewnetrznych bledow serwera — tylko `INTERNAL_ERROR`. Stack trace jest w logach.

### `SecurityConfig` — konfiguracja bezpieczenstwa

Minimalny config na Krok 1:
- CSRF wylaczone (API stateless — cookies nie uzywamy)
- `SessionCreationPolicy.STATELESS` — brak sesji HTTP (uzywamy JWT)
- Publiczne endpointy: `/health`, `/health/ready`, Swagger UI
- Wszystko inne: wymaga uwierzytelnienia

Krok 2 doda: `JwtFilter`, `BCryptPasswordEncoder`, pelnoprawne endpointy auth.

### `HealthController`

Dwa publiczne endpointy dla infrastruktury:
- `GET /api/v1/health` → `{"success": true, "data": {"status": "UP"}}`
- `GET /api/v1/health/ready` → `{"success": true, "data": {"status": "READY"}}`

### `V1__init_schema.sql` — schemat bazy

Pelen schemat ze wszystkimi 19 tabelami z README. Kluczowe decyzje projektowe:
- Uzywamy `UUID` zamiast `BIGSERIAL` dla uzytkownikow i contentow (bezpieczenstwo, rozproszenie)
- `SERIAL` dla `breed` bo to statyczne dane i nie ma potrzeby ukrywania ID
- `friendship_status` jako PostgreSQL `ENUM` zamiast VARCHAR — baza wymusza poprawne wartosci
- Soft delete przez `deleted_at` (nie fizyczne usuniecie) — GDPR + mozliwosc przywrocenia konta
- Denormalizacja `like_count` i `comment_count` w `dog_catch` — szybki odczyt bez JOIN
- Indeksy przy definicji tabel (nie pozniej) — zgodnie z AI_RULES

### `V2__seed_breeds.sql` — danych startowe

10 przykladowych ras psow w roznych grupach i rzadkosciach (1-5). Nie generowane dynamicznie — statyczny SQL zgodnie z AI_RULES.

---

## Weryfikacja zgodnosci z AI_RULES.md

| Zasada | Wynik |
|---|---|
| Pisz kod produkcyjny, nie prototypowy | OK — brak TODO, brak stubów |
| Jeden plik = jedna odpowiedzialnosc | OK — kazda klasa ma jeden cel |
| Brak magic strings i magic numbers | OK — JWT expiry w `application.yml`, kody bledow w `ErrorCode` enum |
| Komentarze opisuja DLACZEGO, nie CO | OK — komentarze przy nieoczywistych decyzjach (np. dlaczego STATELESS) |
| Bledy musza byc obslugiwane | OK — GlobalExceptionHandler, fallback catch, logowanie |
| Brak emoji w kodzie i dokumentacji | OK |
| Sekrety tylko w .env, nie w kodzie | OK — application-dev.yml uzywa `${VAR}` |
| Brak TODO/FIXME | OK |
| Nie uzywa var w Javie | OK — typy jawne wszedzie |
| Logger przez SLF4J | OK — `LoggerFactory.getLogger()`, brak System.out |
| Encje JPA nie eksponowane bezposrednio | OK — na tym etapie brak encji JPA |
| Indeksy w tej samej migracji co tabele | OK — V1 zawiera wszystkie indeksy |

Jeden noter: `application.yml` produkuje ostrzezenie Hibernate o dialekcie (usunelismy jawny dialekt — Hibernate 6 sam go wykrywa). Ostrzezenie nie pojawia sie po tej poprawce.

---

## Weryfikacja dzialania

```bash
# Kontenery uruchomione:
sudo docker compose ps
# petsapp_postgres  healthy  5432
# petsapp_redis     healthy  6379
# petsapp_mailhog   running  1025, 8025
# petsapp_minio     healthy  9000, 9001

# Flyway:
# Successfully validated 2 migrations (V1, V2)
# Schema "public" is up to date.

# Endpointy:
curl http://localhost:8080/api/v1/health
# {"success":true,"data":{"status":"UP"}}

curl http://localhost:8080/api/v1/health/ready
# {"success":true,"data":{"status":"READY"}}

# Swagger UI:
# http://localhost:8080/swagger-ui.html
```

---

## Co NIE zostalo zrobione w Kroku 1 (celowo)

- Brak encji JPA (User, Breed, DogCatch itp.) — to Krok 2+
- Brak testow jednostkowych i integracyjnych — pojawia sie od Kroku 2
- Brak auth (JWT) — Krok 2
- Brak konfiguracji Redis jako cache — Krok 7
- Brak konfiguracji Firebase/FCM — Krok 8

---

## Nastepny krok: Krok 2 — Auth (email + JWT)

Zaczynamy od encji `User`, `RefreshToken`, `EmailVerification`, potem `JwtProvider`, `JwtFilter`, `AuthController` z pelnym flow: rejestracja → weryfikacja email → login → refresh → logout.
