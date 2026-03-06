# Git Flow – Zasady pracy zespołowej

---

## Struktura branchy

```
main          ← stabilna, produkcyjna wersja (tylko Admin merguje!)
└── develop   ← bieżąca wersja deweloperska
    ├── feature/login-page      ← programista 1
    ├── feature/user-profile    ← programista 2
    └── feature/navbar          ← programista 3
```

---

# ADMIN – Jednorazowy setup projektu

> Wykonujesz to tylko raz na początku projektu.

### 1. Stwórz brancha `develop`

```bash
git checkout main
git checkout -b develop
git push origin develop
```

### 2. Zabezpiecz branche na GitHubie

Wejdź w **Settings → Branches → Add branch protection rule** i ustaw osobno dla `main` i `develop`:
- ✅ Require a pull request before merging
- ✅ Require approvals (minimum 1)

Od teraz nikt nie może pushować bezpośrednio na `main` ani `develop`.

### 3. Merge develop → main (nowa wersja produkcyjna)

Gdy `develop` jest stabilny i przetestowany:
1. Wejdź na GitHub → **Pull Requests → New Pull Request**
2. Ustaw: `develop` → `main`
3. Sprawdź zmiany, kliknij **Merge**

---

# PROGRAMISTA – Flow każdego taska

## Krok 1 – Zacznij nowy task

```bash
# Zawsze zaczynaj od aktualnego develop!
git checkout develop
git pull origin develop

# Stwórz swój branch
git checkout -b feature/nazwa-taska
```

> Konwencja nazw: `feature/opis`, `fix/nazwa-buga`

---

## Krok 2 – Pracuj i zapisuj zmiany

```bash
# Sprawdź co zmieniłeś
git status

# Dodaj zmiany
git add .

# Zapisz z opisem
git commit -m "Co zrobiłem"

# Wypchnij na GitHub
git push origin feature/nazwa-taska
```

---

## Krok 3 – Otwórz Pull Request na GitHubie

1. Wejdź na GitHub — pojawi się żółty banner, kliknij **"Compare & pull request"**
2. **WAŻNE:** Upewnij się że ustawiłeś `feature/nazwa-taska` → **`develop`** (NIE `main`!)
3. Dodaj krótki opis co zrobiłeś
4. Kliknij **"Create pull request"**
5. Poczekaj aż ktoś z zespołu zaakceptuje
6. Po akceptacji kliknij **Merge**

---

## Krok 4 – Posprzątaj po merge

```bash
# Wróć na develop
git checkout develop

# Pobierz aktualne zmiany
git pull origin develop

# Usuń lokalny branch
git branch -d feature/nazwa-taska
```

> GitHub zazwyczaj sam zaproponuje usunięcie zdalnego brancha po merge — kliknij **"Delete branch"** na stronie PR-a.

---

## Zasady

| | |
|---|---|
| ✅ | Zawsze rób `git pull origin develop` przed nowym branchem |
| ✅ | PR zawsze kieruj do `develop`, nigdy do `main` |
| ✅ | Commituj często z sensownymi opisami |
| ❌ | Nie pushuj bezpośrednio na `develop` ani `main` |
| ❌ | Nie usuwaj brancha przed mergem PR-a |

---

## Cheatsheet

| Co robię? | Komenda |
|---|---|
| Tworzę nowy branch | `git checkout -b feature/nazwa` |
| Dodaję wszystkie zmiany | `git add .` |
| Commituje | `git commit -m "opis"` |
| Pushuje branch | `git push origin feature/nazwa` |
| Wracam na develop | `git checkout develop` |
| Aktualizuję develop | `git pull origin develop` |
| Usuwam lokalny branch | `git branch -d feature/nazwa` |

---

## Merge Conflict

Pojawia się gdy dwie osoby edytowały ten sam fragment tego samego pliku.

**Jak unikać:**
- Dzielcie pracę tak żeby każda osoba miała swoje pliki/foldery
- Często rób `git pull origin develop`

Jeśli conflict wystąpił — VS Code i IntelliJ mają wbudowane narzędzia do ich rozwiązywania.
