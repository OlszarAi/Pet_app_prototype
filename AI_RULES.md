# AI_RULES.md — Zasady dla AI przy projekcie PetsApp

> **Obowiązkowe.** Każdy prompt do AI (Copilot, Cursor, Gemini, ChatGPT itp.) musi zaczynać się od wskazania tego pliku jako kontekstu, lub bezpośrednio wklejać te zasady.

---

## 1. Ogólne zasady — zawsze

- **Pisz kod produkcyjny, nie prototypowy.** Kazdy plik ma byc gotowy do code review przez seniora.
- **Jeden plik = jedna odpowiedzialnosc.** Nie lacz logiki biznesowej, dostepu do danych i prezentacji w jednym pliku.
- **Brak magic strings i magic numbers.** Uzywaj stalych, enumow i konfiguracji.
- **Nazwy musza mowic same za siebie.** Zadnych `temp`, `data2`, `stuff`, `helper` bez kontekstu.
- **Komentarze opisuja DLACZEGO, nie CO.** Jesli kod wymaga komentarza zeby rozumiec CO robi — przepisz go.
- **Nie duplikuj kodu.** Wyciagnij do wspolnej metody/komponentu zanim skopujesz linie drugi raz.
- **Bledy musza byc obslugiwane.** Zadnych pustych catch blokow, zadnego polykania wyjatkow.
- **Nie generuj kodu z TODO / placeholder / FIXME** — albo implementujesz w pelni, albo pytasz o zakres.
- **Brak emoji w kodzie, komentarzach i dokumentacji.** Emoji to sygnatura AI slopu — kod ma byc profesjonalny i czytelny dla calego zespolu. Jedyny wyjatek: notyfikacje push (tytuly wiadomosci do uzytkownika aplikacji).

---

## 2. Backend — Java / Spring Boot

### Architektura
- Pakiet per feature (`auth/`, `user/`, `catch_/`, `feed/`, ...) — `Controller → Service → Repository`.
- **Controller** — tylko routing, walidacja DTO wejściowych (`@Valid`), mapowanie na Response.
- **Service** — logika biznesowa, transakcje (`@Transactional`).
- **Repository** — tylko dostęp do bazy, zero logiki.
- **DTO** — oddzielne klasy na wejście (`CreateCatchRequest`) i wyjście (`CatchResponse`). Nigdy nie eksponuj encji JPA bezpośrednio.
- Mapowanie encja ↔ DTO — używaj statycznych metod `CatchResponse.from(DogCatch entity)` lub dedykowanego `CatchMapper`.

### Styl kodu
- Google Java Style + Checkstyle + Spotless (sprawdź konfigurację w `pom.xml`).
- `record` dla DTO gdzie to możliwe (Java 21).
- Immutable gdzie się da — `final` pola, brak setterów w encjach JPA (używaj konstruktora/buildera).
- `Optional<T>` zamiast returna `null`.
- Logowanie przez `SLF4J` — `@Slf4j`, bez `System.out.println`.

### Bezpieczeństwo (KRYTYCZNE)
- Każdy endpoint musi mieć jawnie określone uprawnienia w `SecurityConfig`.
- Nigdy nie wkładaj sekretów do kodu — tylko `application.yml` + env vars.
- Zawsze waliduj dane wejściowe przez `@Valid` + Bean Validation.
- Upload zdjęć: whitelist MIME types, max size, strip EXIF — bez wyjątków.
- SQL: tylko JPA/JPQL lub `@Query` z bind parameters — nigdy konkatenacja stringów.

### Testy
- Każda nowa metoda serwisu = test jednostkowy w JUnit 5 + Mockito.
- Każdy nowy endpoint = test integracyjny z Testcontainers (PostgreSQL + Redis).
- Nazwy testów: `methodName_scenario_expectedResult()` np. `register_withExistingEmail_throwsConflictException`.

### Response format
- Zawsze zwracaj `ApiResponse<T>` — wrapper z `success`, `data`, opcjonalnym `pagination`.
- Błędy przez `GlobalExceptionHandler` — nigdy bezpośrednio z kontrolera.
- Kody błędów z enumów `ErrorCode` — nigdy hard-coded stringi.

---

## 3. Frontend — React Native / TypeScript

### Architektura
- Expo Router dla nawigacji (`app/` directory).
- `services/` — tylko wywołania API przez `apiClient`. Żadnych `fetch` bezpośrednio w komponentach.
- `stores/` — tylko globalny stan Zustand. Stan lokalny → `useState` lub `useReducer`.
- `hooks/` — wyciągaj logikę z komponentów do hooków, jeśli to więcej niż 15 linii.
- `types/` — wspólne typy TypeScript. Importuj z jednego miejsca.

### Komponenty
- **Jeden komponent = jeden plik.** Małe komponenty (`< 150 linii`), duże rozbij.
- Props muszą być zawsze otypowane (interface lub type — nie `any`).
- Żadnego `any`. Żadnego `// @ts-ignore` bez komentarza wyjaśniającego.
- Używaj `NativeWind` klas do stylowania — nie twórz inline `style={{}}` dla złożonych layoutów.
- Memoizacja: `React.memo` dla komponentów listy (FlatList items), `useCallback` dla handlerów przekazywanych w dół.

### API / Data fetching
- Wszystkie zapytania przez TanStack Query (`useQuery`, `useMutation`).
- Żadnych bezpośrednich `await fetch()` w komponentach.
- Obsługa stanów: `isLoading`, `isError`, `data` — zawsze wszystkie trzy.
- Optymistyczne update'y (`onMutate`) dla lajków i komentarzy — nie czekaj na serwer.

### TypeScript
- `strict: true` w `tsconfig.json`.
- Typy generuj na podstawie API response lub wspólnych `types/` — nie duplikuj.
- DTO z backendu mają odpowiadające typy w `types/api.ts`.

### Testy
- `jest` + `@testing-library/react-native` dla komponentów.
- Mockuj serwisy, nie implementację HTTP.

---

## 4. Baza danych / Migracje

- **Tylko Flyway** — żadnych zmian schematu ręcznie ani przez `spring.jpa.hibernate.ddl-auto`.
- Pliki: `V{n}__{opis_snake_case}.sql`. Numer sekwencyjny, opis obowiązkowy.
- Migracje są `nieodwracalne` — zawsze przemyśl przed dodaniem `NOT NULL` bez DEFAULT.
- Nowa kolumna → zawsze `nullable` lub z `DEFAULT` w pierwszej migracji (bezpieczny deploy).
- Indeksy dodawaj w tej samej migracji co kolumna/tabela, na której działają.
- Seed data (`V2__seed_breeds.sql`, `V3__seed_achievements.sql`) — statyczne pliki, nie generuj dynamicznie.

---

## 5. Sekrety i bezpieczenstwo repozytorium

> **REPO JEST PUBLICZNE.** Jakikolwiek sekret w kodzie = natychmiastowy incydent bezpieczeństwa.

### Zasady bezwzględne
- **Nigdy** nie commituj haseł, tokenów, kluczy API, connection stringów — nawet dev.
- Wszystkie sekrety trafiają do pliku `.env` (plik jest w `.gitignore`, nie jest commitowany).
- Commituj tylko `.env.example` z wartościami-placeholder (`change_me_dev_only`, `your_key_here`).
- AI generująca kod **musi** używać zmiennych środowiskowych: `${POSTGRES_URL}`, `@Value("${jwt.secret}")`.

### Struktura plików sekretów
```
.env.example   ← commitowany, placeholder wartości
.env           ← NIGDY nie commitowany, realne wartości dev
application-prod.yml  ← NIGDY nie commitowany (jest w .gitignore)
```

### Spring Boot — odczyt sekretów
```yaml
# application-dev.yml (commitowany, tylko localhost wartości przez ${VAR})
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/${POSTGRES_DB}
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}
```

### Jeśli przypadkowo wcommitowałeś sekret
1. Natychmiast unieważnij sekret (zmień hasło/zregeneruj klucz)
2. `git rebase -i` lub `git filter-branch` żeby usunąć z historii
3. Force push na develop/main
4. Powiadom zespół

### Czego AI NIE może robić z sekretami
- ❌ Hardkodować jakichkolwiek wartości w `docker-compose.yml`, `application.yml`, kodzie Java/TS
- ❌ Tworzyć pliku `.env` z realnymi wartościami i proponować jego commit
- ❌ Używać dev credentials w testach — tylko test-specific values przez Testcontainers



- Nie generowac kodu z zakomentowanymi sekcjami "// TODO: implement this later"
- Nie tworzyc pliku `Utils.java` lub `Helpers.ts` jako worka na wszystko
- Nie wklejac logiki biznesowej do kontrolera/komponentu
- Nie uzywac `var` w Javie gdzie typ jest nieoczywisty
- Nie generowac pustych implementacji interfejsow (stub bez tresci)
- Nie tworzyc pliku przekraczajacego ~300 linii bez podzialu na klasy/moduly
- Nie zmieniac istniejacych migracji Flyway (tylko nowa wersja V{n+1})
- Nie hardkowac URL-i, portow, hasel ani kluczy API
- Nie pomijac obslugi bledow (pustych catch, braku .isError w query)
- Nie tworzyc endpointu bez odpowiedniej autoryzacji w SecurityConfig
- Nie uzywac emoji w kodzie, komentarzach, nazwach zmiennych, logow ani dokumentacji technicznej

---

## 6. Jak pracować z AI (workflow)

1. **Daj kontekst.** Wklej opis zadania + wskaż pliki których dotyczy. AI nie zna całego projektu.
2. **Jeden task = jeden PR.** Nie każ AI pisać 5 ficzerów naraz.
3. **Sprawdź output.** Każdy wygenerowany plik przeczytaj przed commitem. AI się myli.
4. **Commit po każdym kroku.** Mały, opisowy commit message (conventional commits).
5. **Nie merguj bez review.** Min. 1 approval od innej osoby — nawet jeśli AI wygenerowało.
6. **Testy przed mergem.** Build musi przechodzić. Testy jednostkowe i integracyjne muszą być zielone.
7. **Opisz w PR co AI zrobiło.** Jeśli używasz AI do generowania kodu, zaznacz to w opisie PR.

---

## 7. Conventional Commits (obowiązkowy format)

```
feat(auth): add email verification flow
fix(catch): handle null breed_id in upload
test(user): add integration test for profile update
refactor(feed): extract scoring logic to FeedScoringService
chore(db): add V4__add_index_on_notification_user.sql
docs(api): update catch endpoint swagger annotations
```

Prefix: `feat|fix|test|refactor|chore|docs|style|ci`
Scope: (`auth|user|catch|feed|friend|notification|achievement|breed|report|infra`)

---

*Plik AI_RULES.md jest źródłem prawdy dla zespołu. Każda zmiana wymaga code review i zgody całego zespołu.*
