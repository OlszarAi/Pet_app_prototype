# PetsApp — Deployment Backendu (Railway + Resend + Cloudflare R2)

Poradnik jak wystawić backend publicznie za darmo używając GitHub Student Pack.

---

## Architektura produkcyjna

```
Frontend (Expo) → Railway Backend (Spring Boot) → Railway PostgreSQL
                                                 → Railway Redis
                                                 → Resend (email)
                                                 → Cloudflare R2 (zdjęcia)
```

### Koszty

| Serwis | Darmowe | Student Pack |
|---|---|---|
| **Railway** | 500h/mies. hobby | $5/mies. kratów (wystarczy na dev) |
| **Cloudflare R2** | 10GB storage, 1M req/mies. | — |
| **Resend.com** | 3000 emaili/mies., 100/dzień | — |
| **Railway PostgreSQL** | W ramach Railway kredytów | — |
| **Railway Redis** | W ramach Railway kredytów | — |

Dla aplikacji dev/beta — wszystko mieści się w darmowych tierach.

---

## Krok 1 — Cloudflare R2 (storage na zdjęcia)

> Zastępuje MinIO. Kompatybilny z S3 API — żadnych zmian w kodzie.

1. Wejdź na https://dash.cloudflare.com → zarejestruj się (darmowe)
2. Lewy pasek → **R2 Object Storage** → **Create bucket**
   - Nazwa: `petsapp-prod`
   - Region: automatyczny
3. Po utworzeniu → **Manage R2 API tokens** → **Create API token**
   - Permissions: `Object Read & Write`
   - Bucket: `petsapp-prod` (specific bucket)
   - Kliknij **Create API Token**
4. **Zapisz** (pojawią się tylko raz!):
   - `Access Key ID` → to jest `CF_R2_ACCESS_KEY`
   - `Secret Access Key` → to jest `CF_R2_SECRET_KEY`
5. Na stronie R2 → znajdź `Account ID` (w URL lub Settings) → to jest `CF_ACCOUNT_ID`

**Ustaw CORS na buckecie** (potrzebne dla przeglądarek):
- R2 bucket → Settings → CORS policy → Add:
```json
[
  {
    "AllowedOrigins": ["*"],
    "AllowedMethods": ["GET"],
    "AllowedHeaders": ["*"],
    "MaxAgeSeconds": 3600
  }
]
```

**Włącz publiczny dostęp do zdjęć:**
- R2 bucket → **Settings** → **Public Access** → Enable
- Skopiuj podany URL, np. `https://pub-abc123def456.r2.dev` → to jest `CF_R2_PUBLIC_URL`

---

## Krok 2 — Resend.com (email)

> Zastępuje MailHog. Prawdziwy email do weryfikacji konta i resetu hasła.

1. Wejdź na https://resend.com → Sign up (GitHub login działa)
2. **Domains** → Add Domain → wpisz swoją domenę (jeśli masz)
   - Jeśli nie masz domeny: możesz wysyłać z `onboarding@resend.dev` na start (tylko na adres, który zarejestrowano w Resend — do testów)
   - Z GitHub Student Pack (Namecheap) możesz dostać **darmową domenę na rok**
3. **API Keys** → Create API Key
   - Name: `petsapp-prod`
   - Permission: `Sending access`
   - Zapisz klucz → to jest `RESEND_API_KEY`

> **Jeśli nie masz domeny na teraz:** W Resend możesz testować bez domeny wysyłając na swój email. Nie blokuje to zadziałania aplikacji.

---

## Krok 3 — Railway (backend + baza + cache)

### 3a. Aktywacja GitHub Student Pack na Railway

1. https://railway.app → Sign up with GitHub
2. Account → **Plans** → **Student Plan** (użyj GitHub Student Pack)
   - Daje $5/mies. kredytów — wystarczy na PostgreSQL + Redis + backend
3. Zweryfikuj przez GitHub Education

### 3b. Stwórz projekt

1. Railway Dashboard → **New Project**
2. Wybierz **Deploy from GitHub repo**
3. Wybierz `Pet_app_prototype` → Railway prosi o dostęp do GitHub
4. **Root Directory** ustaw na `backend` (ważne! bo Dockerfile jest w `backend/`)
5. Kliknij **Deploy Now** (pierwsze deploy może się nie udać bez env vars — normalnie)

### 3c. Dodaj PostgreSQL

1. W projekcie → **New** → **Database** → **Add PostgreSQL**
2. Railway automatycznie tworzy bazę i dodaje `DATABASE_URL` do zmiennych

### 3d. Dodaj Redis

1. W projekcie → **New** → **Database** → **Add Redis**
2. Railway automatycznie dodaje `REDIS_URL` do zmiennych

### 3e. Skonfiguruj zmienne środowiskowe backendu

W Railway → Twoja usługa backendowa → **Variables** → dodaj wszystkie poniższe:

```bash
# Spring profil
SPRING_PROFILES_ACTIVE=prod

# Baza danych — Railway auto-wypełnia DATABASE_URL, ale Spring Boot potrzebuje JDBC URL
# Skopiuj DATABASE_URL z Railway i zmień prefix z postgresql:// na jdbc:postgresql://
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:<port>/<db>
SPRING_DATASOURCE_USERNAME=<user>
SPRING_DATASOURCE_PASSWORD=<password>

# Redis — Upstash lub Railway Redis
# Jeśli Railway Redis: REDIS_URL jest auto-ustawione jako redis://..., ale Spring chce redis://
REDIS_URL=redis://<host>:<port>

# JWT — wygeneruj bezpieczny secret (min 32 znaki)
# Możesz wygenerować: openssl rand -base64 48
JWT_SECRET=wygeneruj_tutaj_dlugi_losowy_string_min_32_znaki

# Email (Resend)
RESEND_API_KEY=re_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# Cloudflare R2
CF_ACCOUNT_ID=twoj_account_id_z_cloudflare
CF_R2_ACCESS_KEY=twoj_access_key_id
CF_R2_SECRET_KEY=twoj_secret_access_key
CF_R2_BUCKET=petsapp-prod
CF_R2_PUBLIC_URL=https://pub-abc123.r2.dev   # z R2 Public Access po włączeniu

# CORS — URL frontendu Expo (po deploymencie frontendu zaktualizuj)
CORS_ALLOWED_ORIGINS=https://twoja-apka.expo.dev,http://localhost:8081
FRONTEND_URL=https://twoja-apka.expo.dev

# Google OAuth (opcjonalne na start)
GOOGLE_CLIENT_ID=twoj_google_client_id
```

> **Jak wygenerować JWT_SECRET:**
> ```bash
> openssl rand -base64 48
> # lub na Windows PowerShell:
> [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
> ```

### 3f. Skopiuj application-prod.yml na serwer

Railway nie czyta pliku `application-prod.yml` z repozytorium (bo jest w `.gitignore`).  
Zamiast tego używamy **zmiennych środowiskowych** ustawionych w kroku 3e — Spring Boot automatycznie zastępuje `${VARIABLE}` wartościami z env.

**Alternatywnie** możesz dodać zmienną `SPRING_APPLICATION_JSON` z całą konfiguracją JSON, ale env vars są prostsze.

---

## Krok 4 — Pierwsze deploy

1. Railway automatycznie buduje po każdym push na `main`
2. Obserwuj logi: Railway → Twój serwis → **Deployments** → kliknij deploy → **View Logs**
3. Szukaj: `Started PetsAppApplication` — to znaczy backend działa
4. Sprawdź health: `https://<twoja-domena>.up.railway.app/api/v1/health`

### Typowe problemy przy pierwszym deploy

**Problem:** `Flyway migration failed`  
**Rozwiązanie:** Sprawdź czy `SPRING_DATASOURCE_URL` ma prefix `jdbc:postgresql://` (nie `postgresql://`)

**Problem:** `Cannot connect to Redis`  
**Rozwiązanie:** Railway Redis URL ma format `redis://default:password@host:port` — upewnij się że `REDIS_URL` jest poprawne

**Problem:** `S3 storage error`  
**Rozwiązanie:** Sprawdź `CF_ACCOUNT_ID`, `CF_R2_ACCESS_KEY`, `CF_R2_SECRET_KEY` — czy bucket `petsapp-prod` istnieje

---

## Krok 5 — Własna domena (opcjonalne)

1. Z GitHub Student Pack → **Namecheap** → darmowa domena `.me` na rok
2. Railway → Twój serwis → **Settings** → **Domains** → **Add Custom Domain**
3. W Namecheap → DNS → dodaj CNAME do Railway
4. Backend będzie dostępny pod np. `api.twojaapka.me`

---

## Krok 6 — Podłączenie frontendu Expo

W pliku konfiguracyjnym frontendu (np. `mobile/services/apiConfig.ts`):

```typescript
const API_BASE_URL = __DEV__
  ? 'http://localhost:8080/api/v1'                          // lokalny dev
  : 'https://twojaapka.up.railway.app/api/v1';              // produkcja Railway

export default API_BASE_URL;
```

Pamiętaj zaktualizować `CORS_ALLOWED_ORIGINS` w Railway po poznaniu URL frontendu!

---

## Automatyczne deploye (CI/CD)

Railway automatycznie deployuje po każdym pushu na `main`:

```bash
# Twoja zmiana w backendzie
git checkout develop
# ... edytuj kod ...
git commit -m "fix: poprawka endpointu"
git push origin develop

# Merge do main → Railway automatycznie deployuje
git checkout main
git merge develop --no-ff -m "chore: deploy fix"
git push origin main
# → Railway wykrywa push, buduje Docker image, deployuje
```

Możesz też skonfigurować Railway by deployował z `develop` (dla szybszych testów).

---

## Monitoring i logi

```
Railway Dashboard → Twój serwis → Logs     # live logi
Railway Dashboard → Twój serwis → Metrics  # CPU, RAM, requests
```

Health check: `GET https://<url>.up.railway.app/api/v1/health`

---

## Podsumowanie env vars (cheat sheet)

| Zmienna | Skąd wziąć |
|---|---|
| `SPRING_DATASOURCE_URL` | Railway → PostgreSQL service → Variables → DATABASE_URL (zmień prefix) |
| `SPRING_DATASOURCE_USERNAME` | Railway → PostgreSQL service → Variables |
| `SPRING_DATASOURCE_PASSWORD` | Railway → PostgreSQL service → Variables |
| `REDIS_URL` | Railway → Redis service → Variables → REDIS_URL |
| `JWT_SECRET` | `openssl rand -base64 48` |
| `RESEND_API_KEY` | resend.com → API Keys |
| `CF_ACCOUNT_ID` | dash.cloudflare.com → prawa strona (Account ID) |
| `CF_R2_ACCESS_KEY` | Cloudflare R2 → Manage API tokens |
| `CF_R2_SECRET_KEY` | Cloudflare R2 → Manage API tokens |
| `CF_R2_BUCKET` | `petsapp-prod` (nazwa bucketu którą stworzyłeś) |
| `CF_R2_PUBLIC_URL` | R2 bucket → Settings → Public Access → podany URL (`https://pub-xxx.r2.dev`) |
| `GOOGLE_CLIENT_ID` | console.cloud.google.com → OAuth credentials |
