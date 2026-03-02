# PetsApp — Współpraca Git (Frontend Developer Guide)

Poradnik dla osoby implementującej frontend — jak pracować z repozytorium, jak nie wejść w konflikt z backendowymi zmianami i jak utrzymywać aktualność kodu.

---

## Struktura branchy

```
main        ← stabilna, zawsze działająca wersja (chroniona, tylko mergowanie PR)
  └── develop      ← główna gałąź developmentu (backend + frontend, linia integracji)
        ├── feature/frontend-auth      ← branch na konkretny ficzer frontendu
        ├── feature/frontend-feed
        ├── feature/frontend-profile
        └── fix/backend-cors           ← hotfixu backendu
```

**Zasada:** Nigdy nie commituj bezpośrednio do `main` ani `develop`. Zawsze przez branch + PR.

---

## Jak zacząć pracę nad frontendem

### 1. Sklonuj repozytorium

```bash
git clone https://github.com/OlszarAi/Pet_app_prototype.git
cd Pet_app_prototype
```

### 2. Zawsze zaczynaj od aktualnego `develop`

```bash
git checkout develop
git pull origin develop       # pobierz najnowsze zmiany
```

### 3. Stwórz branch na swój ficzer

```bash
git checkout -b feature/frontend-auth
# lub
git checkout -b feature/frontend-feed
```

**Konwencja nazewnictwa branchy:**
- `feature/frontend-<nazwa>` — nowa funkcjonalność
- `fix/frontend-<nazwa>` — naprawa buga
- `fix/backend-<nazwa>` — naprawa buga w backendzie

---

## Codziennie: jak synchronizować zmiany backendu

Gdy backend dostaje zmiany (np. nowy endpoint, poprawka), musisz je mieć u siebie. **Rób to codziennie przed pracą:**

```bash
# Będąc na swoim branchu (np. feature/frontend-auth)
git fetch origin                    # pobierz informacje o remote (bez merge'owania)
git rebase origin/develop           # nałóż nowe zmiany develop pod swoje commity
```

Alternatywnie merge (jeśli rebase jest problematyczny):
```bash
git merge origin/develop
```

### Co robić gdy jest konflikt

```bash
# Git pokaże konflikt — otwórz plik, znajdź znaczniki:
# <<<<<<< HEAD (twoje zmiany)
# =======
# >>>>>>> origin/develop (zmiany z develop)

# Edytuj plik, usuń znaczniki, zostaw właściwy kod
# Następnie:
git add <plik>
git rebase --continue    # lub git merge --continue
```

---

## Jak wysłać gotową funkcjonalność

```bash
# 1. Upewnij się że masz najnowszy develop
git fetch origin
git rebase origin/develop

# 2. Wypchnij swój branch
git push origin feature/frontend-auth

# 3. Stwórz Pull Request na GitHub:
#    base: develop  ←  compare: feature/frontend-auth
#    Opis: co zrobiłeś, jak testować, screenshoty (jeśli UI)
```

---

## Jak dostać zmianę w backendzie gdy pracujesz na froncie

Przykładowo: backend dostał poprawkę endpointu `/auth/login` podczas gdy ty piszesz ekran logowania.

```bash
# Sprawdź co się zmieniło
git fetch origin
git log origin/develop..develop --oneline    # jeśli nic nie ma — jesteś aktualny
git log develop..origin/develop --oneline    # to pokaże nowe commity na remote

# Zaaplikuj zmiany
git rebase origin/develop
```

Możesz też ustawić **automatyczne śledzenie**:
```bash
git branch --set-upstream-to=origin/develop develop
```

---

## Struktura projektu — gdzie jest co

```
Pet_app_prototype/
├── backend/           ← Spring Boot — NIE DOTYKAJ bez uzgodnienia
│   └── src/...
├── mobile/            ← React Native (Expo) — TU PISZESZ FRONTEND
│   ├── app/
│   ├── components/
│   ├── hooks/
│   ├── services/      ← API client (tu podłączasz się do backendu)
│   ├── stores/        ← Zustand store
│   └── types/
├── docs/
│   ├── BACKEND.md     ← Dokumentacja backendu (koniecznie przeczytaj!)
│   └── ...
├── docker-compose.yml ← Usługi dev (uruchom przed startem)
└── .env.example       ← Skopiuj do .env
```

---

## Konfiguracja środowiska dev (backend działa lokalnie)

```bash
# 1. Skopiuj env
cp .env.example .env

# 2. Uruchom backend services
docker compose up -d

# 3. Uruchom backend Spring Boot (w osobnym terminalu)
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 4. Backend dostępny pod:
#    API:     http://localhost:8080/api/v1
#    Swagger: http://localhost:8080/api/v1/swagger-ui.html
#    Maile:   http://localhost:8025
```

Pełna dokumentacja backendu: [docs/BACKEND.md](docs/BACKEND.md)

---

## Uruchomienie frontendu (Expo)

```bash
cd mobile
npm install

# Start dev server
npx expo start

# Na iOS (wymaga Mac + Xcode)
npx expo run:ios

# Na Android
npx expo run:android

# W przeglądarce (ograniczone funkcje)
npx expo start --web
```

---

## Konwencje commitów

Używamy [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add login screen
feat: implement feed with infinite scroll
fix: correct token refresh flow
chore: update expo dependencies
docs: add component usage examples
refactor: extract auth hook
```

---

## Kiedy i jak pytać o zmianę w backendzie

Jeśli czegoś potrzebujesz od backendu (nowy endpoint, zmiana response, nowe pole):

1. Sprawdź czy jest już w Swagger UI: http://localhost:8080/api/v1/swagger-ui.html
2. Sprawdź [docs/BACKEND.md](docs/BACKEND.md)
3. Jeśli nie ma → stwórz issue na GitHub lub skontaktuj się

**Nie zmieniaj plików w `backend/` bez uzgodnienia** — zmiany w backendzie mogą wpłynąć na testy i inne moduły.

---

## Tagowanie i wersje

- `v1.0.0-backend` — backend MVP ukończony (kroki 1-8)
- Kolejne tagi przy ukończeniu etapów frontendu

```bash
# Sprawdź dostępne tagi
git tag -l

# Sprawdź co zawiera tag
git show v1.0.0-backend
```

---

## Przydatne komendy

```bash
# Status aktualnego brancha
git status

# Podgląd historii
git log --oneline --graph --all -20

# Co się zmieniło na develop od kiedy zacząłem swój branch
git diff HEAD..origin/develop -- mobile/

# Sprawdź kto zmienił plik
git log --follow -- mobile/services/authApi.ts

# Cofnij ostatni commit (tylko lokalnie!)
git reset --soft HEAD~1

# Porzuć niezapisane zmiany w pliku
git checkout -- mobile/components/SomeComponent.tsx
```
