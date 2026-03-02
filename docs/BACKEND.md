# PetsApp Backend — Dokumentacja Techniczna

> **Stan:** Kroki 1–8 zaimplementowane i przetestowane (136 testów, 0 błędów).
> **Stack:** Java 21 · Spring Boot 3.3 · PostgreSQL 16 · Redis 7 · MinIO (S3 dev)

---

## Spis treści

1. [Szybki start (dev)](#1-szybki-start-dev)
2. [Architektura i struktura pakietów](#2-architektura-i-struktura-pakietów)
3. [Konfiguracja](#3-konfiguracja)
4. [Docker — usługi dev](#4-docker--usługi-dev)
5. [Baza danych i migracje Flyway](#5-baza-danych-i-migracje-flyway)
6. [Wszystkie endpointy API](#6-wszystkie-endpointy-api)
7. [Autentykacja — jak działa JWT](#7-autentykacja--jak-działa-jwt)
8. [Upload zdjęć — flow](#8-upload-zdjęć--flow)
9. [Feed — algorytm rankingowy](#9-feed--algorytm-rankingowy)
10. [Redis — co i jak jest cachowane](#10-redis--co-i-jak-jest-cachowane)
11. [Testy](#11-testy)
12. [Uruchomienie lokalne krok po kroku](#12-uruchomienie-lokalne-krok-po-kroku)
13. [Środowisko produkcyjne — różnice](#13-środowisko-produkcyjne--różnice)

---

## 1. Szybki start (dev)

### Wymagania

| Narzędzie | Wersja |
|---|---|
| Java JDK | 21 |
| Maven | 3.9+ |
| Docker + Docker Compose | v2 |

```bash
# 1. Sklonuj repo i przejdź do katalogu
git clone <repo-url>
cd Pet_app_prototype

# 2. Utwórz plik środowiskowy
cp .env.example .env
# Edytuj .env — zmień hasła (nie musisz zmieniać na dev, ale dobrze to zrobić)

# 3. Uruchom usługi pomocnicze (baza, cache, mail, storage)
docker compose up -d

# 4. Uruchom backend
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 5. Sprawdź działanie
curl http://localhost:8080/api/v1/health
# → {"success":true,"data":{"status":"UP"}}

# Swagger UI (dokumentacja interaktywna)
open http://localhost:8080/api/v1/swagger-ui.html
```

---

## 2. Architektura i struktura pakietów

```
backend/src/main/java/com/petsapp/
│
├── PetsAppApplication.java         # Punkt wejścia Spring Boot
│
├── common/                         # Wspólne utility
│   ├── ApiResponse.java            # Ujednolicony format odpowiedzi {success, data, error}
│   ├── ErrorCode.java              # Enum kodów błędów (VALIDATION_ERROR, NOT_FOUND, ...)
│   └── GlobalExceptionHandler.java # @ControllerAdvice — obsługa wyjątków → ApiResponse
│
├── config/                         # Całość konfiguracji Spring
│   ├── SecurityConfig.java         # Spring Security + JWT filter, reguły dostępu
│   ├── RedisConfig.java            # Konfiguracja RedisTemplate + StringRedisTemplate
│   ├── AsyncConfig.java            # ThreadPoolTaskExecutor dla @Async (push, achievements)
│   └── UserDetailsServiceImpl.java # Ładuje usera z DB po UUID dla Spring Security
│
├── auth/                           # Moduł autentykacji
│   ├── User.java                   # Encja JPA tabela "user"
│   ├── AuthController.java         # /auth/* endpointy
│   ├── AuthService.java            # register, verify, login, refresh, logout
│   ├── JwtProvider.java            # generuje i waliduje tokeny JWT (HMAC-SHA256)
│   ├── JwtFilter.java              # OncePerRequestFilter — czyta Bearer token z nagłówka
│   ├── OAuthService.java           # Google OAuth2 — find-or-create user
│   ├── EmailService.java           # Wysyłka emaili przez Spring Mail (MailHog na dev)
│   ├── RateLimitService.java       # Bucket4j + Redis — limity req/min per endpoint
│   ├── RefreshToken.java
│   ├── EmailVerification.java
│   ├── PasswordReset.java
│   └── UserSettings.java
│
├── user/                           # Moduł użytkownika
│   ├── UserController.java         # /users/* endpointy profilu
│   └── UserService.java            # edycja profilu, zmiana hasła, avatar, soft delete
│
├── breed/                          # Moduł ras psów
│   ├── Breed.java                  # Encja — ~200 ras z rarity_score (1-5)
│   ├── BreedController.java        # /breeds — lista, szczegóły
│   ├── BreedService.java
│   ├── PokedexController.java      # /users/{id}/pokedex — odkryte rasy
│   └── PokedexStatsResponse.java   # postęp, streak, ulubiona rasa
│
├── catch_/                         # Moduł fotografowania psów (catch_ bo catch jest słowem kluczowym Java)
│   ├── DogCatch.java               # Encja — zdjęcie psa + metadane
│   ├── CatchController.java        # /catches — upload, lajki, komentarze, raporty
│   ├── CatchService.java
│   ├── ImageResizer.java           # Thumbnailator — resize 800px + 200px thumb, strip EXIF
│   ├── Like.java
│   ├── Comment.java
│   └── CatchReport.java
│
├── feed/                           # Moduł feedu
│   ├── FeedController.java         # /feed/public, /feed/friends, /feed/trending
│   ├── FeedService.java            # Logika pobierania feedu z Redis
│   ├── FeedRankingService.java     # Oblicza i aktualizuje feed_score
│   ├── FeedCacheService.java       # Zarządza Redis Sorted Sets
│   └── FeedRebuildJob.java         # @Scheduled co 15 min — pełny rebuild scorów
│
├── friend/                         # Moduł znajomych
│   ├── Friendship.java             # Encja — status: PENDING/ACCEPTED/BLOCKED
│   ├── FriendController.java       # /friends — request, accept, reject, block
│   └── FriendService.java
│
├── notification/                   # Moduł powiadomień
│   ├── Notification.java           # Encja — powiadomienia in-app
│   ├── DeviceToken.java            # Encja — tokeny FCM/APNs
│   ├── NotificationController.java # /notifications + /devices
│   ├── NotificationService.java    # Tworzenie i odczyt powiadomień
│   └── PushService.java            # Firebase Admin SDK (stub na dev — nie wysyła realnie)
│
├── achievement/                    # Moduł achievementów
│   ├── Achievement.java            # Encja — 9 achievementów z tabeli seed
│   ├── AchievementChecker.java     # @Async — sprawdza warunki po każdym POST /catches
│   └── AchievementController.java  # /achievements — lista + odblokowane
│
├── storage/                        # Serwis przechowywania plików
│   └──S3StorageService.java        # MinIO (dev) / AWS S3 (prod) — upload, delete, presigned URL
│
└── health/
    └── HealthController.java       # GET /health, GET /health/ready
```

---

## 3. Konfiguracja

### Pliki konfiguracyjne

| Plik | Przeznaczenie |
|---|---|
| `application.yml` | Bazowa konfiguracja — bez sekretów, profile-neutral |
| `application-dev.yml` | Dev override — localhost DB/Redis, MinIO, debug logging |
| `application-prod.yml` | Prod override — **nie commitowany do repo** (w .gitignore) |
| `.env` | Sekrety (hasła, JWT secret) — **nie commitowany** |
| `.env.example` | Szablon — commitowany, bez prawdziwych wartości |

### Zmienne środowiskowe (`.env`)

```bash
# PostgreSQL
POSTGRES_DB=petsapp_dev
POSTGRES_USER=petsapp
POSTGRES_PASSWORD=twoje_haslo

# JWT
JWT_SECRET=min_32_znakow_zmien_na_cos_bezpiecznego

# MinIO (lokalne S3)
MINIO_ROOT_USER=petsapp_minio
MINIO_ROOT_PASSWORD=twoje_haslo_minio

# Opcjonalne (dev)
GOOGLE_CLIENT_ID=twoj_google_client_id   # potrzebne tylko do testowania OAuth
```

### Kluczowe parametry aplikacji (`application.yml`)

| Parametr | Wartość | Opis |
|---|---|---|
| `app.jwt.access-token-expiration-ms` | `900000` | Access token: 15 min |
| `app.jwt.refresh-token-expiration-ms` | `604800000` | Refresh token: 7 dni |
| `server.servlet.context-path` | `/api/v1` | Prefix wszystkich endpointów |
| `spring.servlet.multipart.max-file-size` | `10MB` | Limit rozmiaru zdjęcia |

---

## 4. Docker — usługi dev

Plik: [docker-compose.yml](../docker-compose.yml)

```
docker compose up -d      # start wszystkich usług
docker compose down       # stop (zachowuje dane)
docker compose down -v    # stop + usuwa wolumeny (reset bazy!)
docker compose ps         # sprawdź status
docker compose logs -f    # śledź logi
docker compose logs -f postgres  # logi konkretnej usługi
```

### Uruchomione usługi

| Usługa | Port | Interfejs webowy | Opis |
|---|---|---|---|
| PostgreSQL 16 | `5432` | — | Główna baza danych |
| Redis 7 | `6379` | — | Cache feed, rate limiting, leaderboard |
| MailHog | `1025` (SMTP), `8025` (Web) | http://localhost:8025 | Przechwytuje emaile (weryfikacja, reset hasła) |
| MinIO | `9000` (S3 API), `9001` (Console) | http://localhost:9001 | Lokalne storage S3 dla zdjęć |

### Jak sprawdzić email wysłany przez aplikację

1. Otwórz http://localhost:8025
2. Wszystkie emaile wysłane przez backend pojawią się tutaj w czasie rzeczywistym
3. Działa dla: weryfikacji emaila, resetu hasła

### Jak sprawdzić przechowywane zdjęcia (MinIO)

1. Otwórz http://localhost:9001
2. Login: wartości z `.env` (`MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD`)
3. Bucket: `petsapp-dev`

---

## 5. Baza danych i migracje Flyway

Flyway automatycznie uruchamia migracje przy starcie aplikacji.

### Migracje

| Plik | Zawartość |
|---|---|
| `V1__init_schema.sql` | Pełny schemat — wszystkie tabele, indeksy, constrainty |
| `V2__seed_breeds.sql` | Pierwsze ~15 ras (Golden Retriever, Labrador, ...) |
| `V3__seed_breeds_extended.sql` | Rozszerzone seedy ras — łącznie ~50 ras |
| `V4__seed_achievements.sql` | 9 achievementów MVP |
| `V5__fix_friendship_status_column.sql` | Poprawka kolumny statusu znajomości |

### Komendy Flyway

```bash
cd backend

# Sprawdź stan migracji (co zastosowane, co oczekuje)
mvn flyway:info -Dspring-boot.run.profiles=dev

# Ręczne zastosowanie migracji (normalnie robi to Spring Boot przy starcie)
mvn flyway:migrate -Dspring-boot.run.profiles=dev

# Reset bazy DEV (uwaga: niszczy dane!)
docker compose down -v
docker compose up -d postgres
mvn spring-boot:run -Dspring-boot.run.profiles=dev  # Flyway sam odtworzy schemat
```

### Tabele główne

```
"user"              — konta użytkowników (email, username, BCrypt hash, statystyki denorm.)
dog_catch           — zdjęcia psów (photo_url, thumb_url, breed_id, feed_score)
breed               — ~50 ras psów (name, group_fci, rarity_score 1-5, silhouette_url)
friendship          — relacje znajomi (requester_id, addressee_id, status: PENDING/ACCEPTED/BLOCKED)
"like"              — polubienia catchów
comment             — komentarze do catchów
notification        — powiadomienia in-app
device_token        — tokeny FCM/APNs dla push
achievement         — definicje achievementów
achievement_unlock  — odblokowane achievementy per user
refresh_token       — aktywne refresh tokeny (dla możliwości logout)
email_verification  — kody weryfikacyjne emaila
password_reset      — tokeny resetu hasła (ważne 1h)
user_settings       — ustawienia usera (push, prywatność, dark mode)
report              — zgłoszenia nieodpowiednich treści
```

---

## 6. Wszystkie endpointy API

Bazowy URL: `http://localhost:8080/api/v1`

Swagger UI (interaktywna dokumentacja): http://localhost:8080/api/v1/swagger-ui.html

### Format odpowiedzi

Każdy endpoint zwraca ten sam envelope:

```json
// Sukces
{ "success": true, "data": { ... } }

// Lista z paginacją
{ "success": true, "data": [...], "pagination": { "cursor": "abc", "hasMore": true } }

// Błąd
{ "success": false, "error": { "code": "NOT_FOUND", "message": "Resource not found" } }
```

### Auth `/auth`

| Metoda | Ścieżka | Auth | Opis |
|---|---|---|---|
| POST | `/auth/register` | — | Rejestracja: email + username + hasło → wysyła kod na email |
| POST | `/auth/verify-email` | — | Weryfikacja emaila 6-cyfrowym kodem |
| POST | `/auth/resend-verification` | — | Ponowne wysłanie kodu |
| POST | `/auth/login` | — | Login → `{ accessToken, refreshToken }` |
| POST | `/auth/refresh` | — | Nowy access token z refresh tokena |
| POST | `/auth/logout` | Bearer | Unieważnienie refresh tokena |
| POST | `/auth/forgot-password` | — | Wysyła link restu hasła (ważny 1h) |
| POST | `/auth/reset-password` | — | Zmiana hasła tokenem z emaila |
| POST | `/auth/google` | — | Logowanie Google: `{ idToken }` → `{ accessToken, refreshToken }` |

### Użytkownicy `/users`

| Metoda | Ścieżka | Auth | Opis |
|---|---|---|---|
| GET | `/users/me` | Bearer | Mój profil (statystyki, bio, avatar) |
| PATCH | `/users/me` | Bearer | Edycja username/bio |
| POST | `/users/me/avatar` | Bearer | Upload zdjęcia profilowego (multipart) |
| DELETE | `/users/me/avatar` | Bearer | Usunięcie avatara |
| PATCH | `/users/me/password` | Bearer | Zmiana hasła (wymagane stare hasło) |
| DELETE | `/users/me` | Bearer | Usunięcie konta (soft delete, 30 dni na przywrócenie) |
| GET | `/users/me/settings` | Bearer | Pobierz ustawienia |
| PATCH | `/users/me/settings` | Bearer | Zmień ustawienia (push, prywatność, dark mode) |
| GET | `/users/search?q=` | Bearer | Wyszukaj po username |
| GET | `/users/{id}` | Bearer | Profil innego usera |
| GET | `/users/{id}/catches` | Bearer | Zdjęcia innego usera (grid, cursor pagination) |
| GET | `/users/{id}/achievements` | Bearer | Achievementy innego usera |

### Rasy / Pokédex

| Metoda | Ścieżka | Auth | Opis |
|---|---|---|---|
| GET | `/breeds` | Bearer | Lista ras (`?q=`, `?group=`, `?size=`) |
| GET | `/breeds/{id}` | Bearer | Szczegóły rasy + globalne statystyki |
| GET | `/users/{id}/pokedex` | Bearer | Odkryte rasy usera |
| GET | `/users/{id}/pokedex/stats` | Bearer | Statystyki Pokédexu (postęp, streak) |

### Łapanie psów

| Metoda | Ścieżka | Auth | Opis |
|---|---|---|---|
| POST | `/catches` | Bearer | Nowe złapanie (multipart: photo + breed_id + caption + is_public) |
| GET | `/catches/{id}` | Bearer | Szczegóły złapania |
| DELETE | `/catches/{id}` | Bearer | Usuń swoje złapanie (soft delete) |
| POST | `/catches/{id}/likes` | Bearer | Polub |
| DELETE | `/catches/{id}/likes` | Bearer | Cofnij polubienie |
| GET | `/catches/{id}/comments` | Bearer | Komentarze (cursor pagination) |
| POST | `/catches/{id}/comments` | Bearer | Dodaj komentarz |
| DELETE | `/comments/{id}` | Bearer | Usuń swój komentarz |
| POST | `/catches/{id}/reports` | Bearer | Zgłoś złapanie |

### Feed

| Metoda | Ścieżka | Auth | Opis |
|---|---|---|---|
| GET | `/feed/public?cursor=&limit=20` | Bearer | Feed publiczny (sorted by feed_score) |
| GET | `/feed/friends?cursor=&limit=20` | Bearer | Feed znajomych (chronologiczny) |
| GET | `/feed/trending?limit=10` | Bearer | Top 10 z ostatnich 24h |

### Znajomi

| Metoda | Ścieżka | Auth | Opis |
|---|---|---|---|
| GET | `/friends` | Bearer | Lista znajomych |
| POST | `/friends/requests` | Bearer | Wyślij zaproszenie `{ userId }` |
| PATCH | `/friends/requests/{id}/accept` | Bearer | Akceptuj zaproszenie |
| DELETE | `/friends/requests/{id}` | Bearer | Odrzuć / anuluj zaproszenie |
| GET | `/friends/requests/pending` | Bearer | Oczekujące zaproszenia |
| GET | `/friends/leaderboard` | Bearer | Ranking znajomych (unique_breeds DESC) |
| POST | `/users/block/{userId}` | Bearer | Zablokuj usera |
| DELETE | `/users/{userId}` | Bearer | Odblokuj usera |

### Powiadomienia

| Metoda | Ścieżka | Auth | Opis |
|---|---|---|---|
| GET | `/notifications` | Bearer | Lista powiadomień (cursor pagination) |
| POST | `/notifications/read-all` | Bearer | Oznacz wszystkie jako przeczytane |
| GET | `/users/me/achievements` | Bearer | Moje achievementy |
| POST | `/devices` | Bearer | Rejestracja tokena FCM/APNs |
| DELETE | `/devices/{token}` | Bearer | Usuń token urządzenia (przy logout) |

### Achievementy

| Metoda | Ścieżka | Auth | Opis |
|---|---|---|---|
| GET | `/achievements` | Bearer | Wszystkie achievementy (z info czy odblokowane) |

### System

| Metoda | Ścieżka | Auth | Opis |
|---|---|---|---|
| GET | `/health` | — | Health check (DB + Redis + S3 status) |
| GET | `/health/ready` | — | Readiness check |

---

## 7. Autentykacja — jak działa JWT

### Flow rejestracji

```
POST /auth/register { email, username, password }
  → walidacja (email unikalny, username unikalny, hasło min 8 znaków)
  → hash BCrypt cost=12
  → zapis do DB (email_verified=false)
  → email z 6-cyfrowym kodem (ważny 15 min)

POST /auth/verify-email { email, code }
  → aktywacja konta (email_verified=true)
  → zwrot: { accessToken, refreshToken }
```

### Używanie tokenów w API

```
Authorization: Bearer <accessToken>
```

- Access token: ważny **15 minut** (JWT, HMAC-SHA256)
- Refresh token: ważny **7 dni** (przechowywany w DB, hashowany SHA-256)
- Po wygaśnięciu access tokena: `POST /auth/refresh { refreshToken }` → nowy access token

### Rate limiting

| Endpoint | Limit |
|---|---|
| `POST /auth/login` | 5 req/min per IP |
| `POST /auth/register` | 3 req/min per IP |
| `POST /catches` (upload) | 10 req/min per user |
| Pozostałe | 100 req/min ogólne |

---

## 8. Upload zdjęć — flow

```
POST /catches (multipart/form-data)
  Fields: photo (file), breedId (UUID), caption (string), isPublic (boolean)

Backend:
  1. Walidacja MIME type → tylko image/jpeg, image/png, image/webp
  2. Max 10MB
  3. Strip EXIF metadata (prywatność lokalizacji)
  4. Resize → 800px szerokości (photo_url)
  5. Resize → 200px szerokości (thumb_url) 
  6. Konwersja do WebP
  7. Upload do MinIO/S3 → petsapp-dev/catches/{uuid}.webp
  8. Zapis do DB (dog_catch) z presigned URL
  9. Aktualizacja statystyk usera (denormalizacja: total_catches, unique_breeds)
  10. @Async: AchievementChecker → sprawdza czy odblokowano nowe achievement
  11. @Async: Aktualizacja feed score w Redis
```

---

## 9. Feed — algorytm rankingowy

### Formuła

```
feed_score = (likes * 1.0 + comments * 2.0 + rarity_bonus) / (hours_since_post + 2)^1.5

gdzie:
  rarity_bonus = breed.rarity_score * 3   (rasy rare mają boost)
  hours_since_post = czas od uploadu w godzinach
```

### Typy feedu

| Typ | Endpoint | Jak działa |
|---|---|---|
| **Publiczny** | `/feed/public` | Wszystkie publiczne zdjęcia, sorted by `feed_score` DESC |
| **Znajomi** | `/feed/friends` | Tylko zdjęcia znajomych, chronologiczny |
| **Trending** | `/feed/trending` | Top 10 z ostatnich 24h, refresh co 5 min |

### Paginacja

Wszystkie listy używają **cursor pagination** (nie offset/page):
```
GET /feed/public?cursor=abc123&limit=20
→ response: { data: [...], pagination: { cursor: "def456", hasMore: true } }
```

---

## 10. Redis — co i jak jest cachowane

| Klucz Redis | Typ | TTL | Zawartość |
|---|---|---|---|
| `feed:public` | Sorted Set | 15 min | catch_id → feed_score (wszystkie publiczne) |
| `feed:friends:{userId}` | Sorted Set | 5 min | catch_id → timestamp (catch znajomych) |
| `feed:trending` | Sorted Set | 5 min | catch_id → feed_score (top 24h) |
| `leaderboard:friends:{userId}` | Sorted Set | 1h | user_id → unique_breeds count |
| `rate_limit:{ip}:{endpoint}` | Counter | 1 min | Bucket4j rate limiter |

### FeedRebuildJob

Scheduled co 15 minut — przelicza feed_score dla wszystkich aktywnych catchów z ostatnich 7 dni i aktualizuje Redis Sorted Sets.

---

## 11. Testy

### Uruchamianie testów

```bash
cd backend

# Wszystkie testy (wymaga działającego Docker — Testcontainers)
mvn test

# Konkretny test
mvn test -Dtest="AuthIntegrationTest"
mvn test -Dtest="FeedServiceTest"
mvn test -Dtest="FriendIntegrationTest"

# Z verbose output
mvn test -Dtest="AchievementCheckerTest" -pl backend
```

### Pokrycie testami (136 testów, 0 błędów)

| Moduł | Klasa testowa | Testy |
|---|---|---|
| Auth (rejestracja, login, tokeny) | `AuthIntegrationTest` | ~10 |
| Auth (reset hasła) | `PasswordResetIntegrationTest` | 7 |
| Auth (Google OAuth) | `OAuthServiceTest` | ~5 |
| Rasy i Pokédex | `BreedIntegrationTest` | ~8 |
| Łapanie psów | `CatchIntegrationTest` | ~15 |
| Feed (serwis) | `FeedServiceTest` | ~12 |
| Feed (ranking) | `FeedRankingServiceTest` | 8 |
| Feed (integracyjny) | `FeedIntegrationTest` | ~12 |
| Znajomi | `FriendIntegrationTest` | ~20 |
| Powiadomienia | `NotificationServiceTest` | 6 |
| Achievementy | `AchievementCheckerTest` | 7 |
| Profil / ustawienia | `UserIntegrationTest`, `UserSettingsIntegrationTest` | ~16 |

### Infrastructure testów

- **Testcontainers** — automatycznie uruchamia PostgreSQL w Docker na czas testów (nie potrzeba lokalnej bazy)
- Testy integracyjne dziedziczą po `AbstractIntegrationTest` — wspólna inicjalizacja, cleanup
- Testy mockują: EmailService (brak prawdziwych emaili), PushService (Firebase stub), S3StorageService (nie potrzeba MinIO)

### Sprawdzenie wyników ostatniego build

```bash
# Szybki przegląd wyników surefire
grep "Tests run:" backend/target/surefire-reports/*.txt

# Sprawdź czy są błędy
grep -l "FAILED\|ERROR" backend/target/surefire-reports/*.txt
```

---

## 12. Uruchomienie lokalne krok po kroku

### Pierwsze uruchomienie (od zera)

```bash
# 1. Prerequisites
java -version    # musi być 21+
mvn -version     # musi być 3.9+
docker -version  # musi być dostępny

# 2. .env
cp .env.example .env
# Edytuj .env — zmień POSTGRES_PASSWORD, MINIO_ROOT_PASSWORD, JWT_SECRET

# 3. Docker
docker compose up -d
docker compose ps  # wszystkie powinny być "healthy" po ~30s

# 4. Build i uruchomienie backendu
cd backend
mvn clean package -DskipTests  # kompilacja bez testów (szybko)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# lub uruchom skompilowany JAR
java -jar target/petsapp-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

# 5. MinIO — utwórz bucket (raz)
# Otwórz http://localhost:9001, zaloguj się, utwórz bucket "petsapp-dev"
# lub przez mc CLI:
# mc alias set local http://localhost:9000 petsapp_minio <twoje_haslo>
# mc mb local/petsapp-dev
```

### Codzienne uruchamianie

```bash
# Start usług (jeśli zatrzymane)
docker compose up -d

# Start backendu
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Porty

| Usługa | URL/Port |
|---|---|
| Backend API | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/api/v1/swagger-ui.html |
| PostgreSQL | localhost:5432 |
| Redis | localhost:6379 |
| MailHog | http://localhost:8025 |
| MinIO Console | http://localhost:9001 |
| MinIO S3 API | http://localhost:9000 |

---

## 13. Środowisko produkcyjne — różnice

> Krok 13 w planie implementacji — na razie dokumentacja referencyjna.

| Aspekt | Dev | Prod |
|---|---|---|
| Storage | MinIO (lokalnie) | AWS S3 lub Cloudflare R2 |
| Email | MailHog (localhost) | Resend lub AWS SES |
| Push notifications | Stub (loguje, nie wysyła) | Firebase Admin SDK (prawdziwy) |
| DB | PostgreSQL w Docker | Managed PostgreSQL (RDS, Supabase) |
| Sekrety | plik `.env` | AWS Secrets Manager / Railway Env |
| Spring Profile | `dev` | `prod` |
| `application-prod.yml` | nie istnieje lokalnie | w gitignore — uzupełnione przez CI/CD |

### Zmiany dla prod (`application-prod.yml`)

```yaml
spring:
  config:
    activate:
      on-profile: prod
  datasource:
    url: ${DATABASE_URL}
  data:
    redis:
      url: ${REDIS_URL}
  mail:
    host: smtp.resend.com
    port: 465

app:
  storage:
    type: s3
    region: eu-central-1
    bucket: petsapp-prod
  frontend-url: https://twoja-domena.app
```
