# pojebane gówno

> **Ważne:** Backend aplikacji jest napisany w Javie i serwuje API.
> Twoim zadaniem jest napisanie aplikacji mobilnej, która z nim rozmawia.

---

## Co będziesz budować?

PetsApp to aplikacja mobilna do kolekcjonowania zdjęć psów według ras.
User fotografuje psy na spacerze, buduje **Zbiór ras**, dzieli się
zdjęciami ze znajomymi w feedzie i odblokowuje achievementy.

Na backendzie pracuje już baza danych, system autoryzacji JWT, upload zdjęć na S3
i algorytm rankingu feedu. Twoją rolą jest napisanie **interfejsu** — tego co
użytkowniczk widzi i dotyka.

---

## Mapa nauki (3 etapy)

```
ETAP 1 — Nauka przez budowanie (tu spędzisz najwięcej czasu :) :) :) )
  Lekcja 0:  Konfiguracja + Twój pierwszy działający projekt
  Lekcja 1:  JavaScript — zmienne i funkcje
  Lekcja 2:  Tablice i obiekty
  Lekcja 3:  Twój pierwszy komponent React Native
  Lekcja 4:  Props — jak komponenty rozmawiają ze sobą
  Lekcja 5:  useState — komponent z pamięcią
  Lekcja 6:  FlatList — wyświetlanie list
  Lekcja 7:  useEffect — "uruchom to po załadowaniu ekranu"
  Lekcja 8:  async/await — rozmowa z serwerem
  Lekcja 9:  Struktura projektu — jak dzielić kod na pliki ← KLUCZOWA
  Projekt 1: Tamagotchi App (zbiera wszystko z Etapu 1)

ETAP 2 — Wzorce produkcyjne
  Lekcja 10: Expo Router — nawigacja przez pliki
  Lekcja 11: TanStack Query — profesjonalna komunikacja z API
  Lekcja 12: Zustand — globalny stan aplikacji
  Lekcja 13: NativeWind — stylowanie przez klasy
  Projekt 2: BrowserRas — podłączenie do żywego backendu PetsApp

ETAP 3 — Wdrożenie PetsApp
  Krok 9:  Ekrany autoryzacji (login, rejestracja, weryfikacja emaila)
  Krok 10: Feed + aparat + upload zdjęcia
  Krok 11: Pokedex ras
  Krok 12: Profil, znajomi, achievementy, push notifications
```

---

# ETAP 1 — Nauka przez budowanie z przykladami itp

---

## Lekcja 0 — Konfiguracja środowiska

### Co musisz zainstalować

#### 1. Node.js (silnik JavaScript)

```bash
# Sprawdź czy masz (wymagana wersja 20+)
node --version
npm --version
```

Jeśli `command not found` — pobierz instalator ze strony: **https://nodejs.org**
(wybierz wersję **LTS**, kliknij `.pkg` dla macOS i przejdź przez kreator).
Node.js zawiera już `npm` w sobie — instalujesz jedno i masz oba.

Po instalacji zrestartuj terminal i sprawdź:
```bash
node --version   # powinno pokazać v20.x.x lub nowsze
npm --version    # powinno pokazać 10.x.x lub nowsze
```

#### 2. VS Code + rozszerzenia

Pobierz z: https://code.visualstudio.com (wersja `.dmg` dla macOS)
albo przez Homebrew: `brew install --cask visual-studio-code`

Zainstaluj rozszerzenia (⌘+Shift+X → wpisz nazwę):
- `ESLint` — podkreśla błędy w kodzie na czerwono
- `Prettier - Code formatter` — formatuje kod po ⌘+S
- `React Native Tools` — podświetlanie składni
- `TypeScript Hero` — pomocnik TypeScript

**Ustaw formatowanie przy zapisie:** Otwórz ustawienia (⌘+,), wyszukaj
`format on save` i zaznacz checkbox.

#### 3. Expo Go na telefonie

Pobierz ze sklepu App Store lub Google Play aplikację **"Expo Go"**.
w tej apce bedziesz widziec swoja apke na zywo, gdybysmy buildowali w czystym react bys mogla to widziec na pc ale niestety tutaj tak trzeba, da sie zrobic zeby widziec to na pc ale to ci pokaze i tak dobrze miec te expo go.

### Tworzysz PIERWSZY projekt — zacznijmy od razu
to piszesz w terminalu na dole VSC jak go nie masz to na gorze terminal -> new terminal. musisz byc w folderze apki zeby stworzyc projekt.
```bash
# Utwórz projekt z szablonem TypeScript
npx create-expo-app MojaApka --template expo-template-blank-typescript

cd MojaApka

# Uruchom
npm start
```

W terminalu pojawi się kod QR. Zeskanuj go aplikacją **Expo Go** na telefonie —
zobaczysz biały ekran z tekstem. To jest Twoja aplikacja mobilna. Działa!

Zmień tekst w `App.tsx`, zapisz (⌘+S) — aplikacja na telefonie odświeży się
**automatycznie**. To się nazywa **Hot Reload**.

Otwórz `App.tsx` i przyjrzyj się strukturze:

```tsx
// App.tsx — to jest twój cały projekt na razie
import { StatusBar } from 'expo-status-bar';
import { StyleSheet, Text, View } from 'react-native';

export default function App() {
  return (
    <View style={styles.container}>
      <Text>Open up App.tsx to start working on your app!</Text>
      <StatusBar style="auto" />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,                      // zajmij całą dostępną przestrzeń
    backgroundColor: '#fff',
    alignItems: 'center',         // wycentruj w poziomie
    justifyContent: 'center',     // wycentruj w pionie
  },
});
```

---

> ### ZADANIE 0
> Zmień kolor tła na niebieski (`'#3B82F6'`), tekst na `'test 123 dziala? dziala'`
> i kolor tekstu na biały.
> Upewnij się że zmiana jest widoczna na telefonie.
> **Nie idź dalej zanim tego nie zrobisz.**

---

## Lekcja 1 — JavaScript: Zmienne i funkcje

Otwórz `App.tsx`. Wszystkie przykłady z tej lekcji wpisuj **powyżej** funkcji `App`,
żeby od razu widzieć że kod się kompiluje (brak czerwonych podkreśleń = sukces).

### Zmienne — `const` i `let`

```typescript
// const — przypisanie raz na zawsze (jak etykieta naklejona na pudełko)
const nazwaAplikacji = "PetsApp";
const wersja = 1;
const czyJestProdukcja = false;

// let — wartość może się zmienić
let liczbaUzytkownikow = 0;
liczbaUzytkownikow = 1;     // OK — możemy zmienić
liczbaUzytkownikow += 5;    // skrót: liczbaUzytkownikow = liczbaUzytkownikow + 5

// NIGDY nie używaj var — ma przestarzałe zachowanie zasięgu
```

**Pułapka z `const` i obiektami** — zapamiętaj raz na zawsze:

```typescript
// const NIE znaczy "niezmienialny" dla obiektów i tablic!
const pies = { imie: "Burek", rasa: "Labrador" };
pies.rasa = "Husky";    // ✅ OK — zmieniamy właściwość wewnątrz obiektu
// pies = { imie: "Azor" }; // ❌ BŁĄD — próba zamiany CAŁEGO obiektu

const rasy = ["Labrador", "Husky"];
rasy.push("Beagle");    // ✅ OK — dodajemy do środka tablicy
// rasy = ["Poodle"];  // ❌ BŁĄD — próba zamiany CAŁEJ tablicy
```

### Funkcje — trzy sposoby zapisu

```typescript
// Sposób 1: Klasyczna deklaracja funkcji
function powiedzCzesc(imie: string): string {
  return `Cześć, ${imie}!`;
}

// Sposób 2: Arrow function (UŻYWAJ TEGO — standardowy zapis w React)
const powiedzCzesc2 = (imie: string): string => {
  return `Cześć, ${imie}!`;
};

// Sposób 3: Arrow function — skrócona (jednolinijkowy return)
const powiedzCzesc3 = (imie: string): string => `Cześć, ${imie}!`;

// Template literals — wstawianie zmiennych do tekstu (backtick, nie cudzysłów!)
const imie = "Zuzia";
const wiek = 25;
console.log(`Cześć, ${imie}! Masz ${wiek} lat.`);  // "Cześć, Zuzia! Masz 25 lat."
console.log(`2 + 2 = ${2 + 2}`);                     // "2 + 2 = 4"
```

**Dlaczego arrow functions są standardem w React?**
Mają krótszy zapis i nie tworzą własnego `this` — co ma znaczenie gdy przekazujesz
funkcje jako props (zobaczysz to w Lekcji 4).

### Zasięg zmiennych (scope) — gdzie żyje zmienna?

Zmienna istnieje tylko w bloku `{}` w którym ją zadeklarowałaś. Poza nim —
niedostępna. To się nazywa **block scope** i dotyczy `const` oraz `let`.

```typescript
function przykladScope() {
  const zewnetrzna = "widzę wszystkich";  // dostępna w całej funkcji

  if (true) {
    const wewnetrzna = "tylko tu";         // dostępna tylko w tym if-bloku
    let tezWewnetrzna = "też tylko tu";
    console.log(zewnetrzna);               // ✅ widzi zmienną z zewnątrz
    console.log(wewnetrzna);               // ✅
  }

  // console.log(wewnetrzna);             // ❌ BŁĄD — poza zakresem bloku
}
```

### Zmienne globalne — czym są i dlaczego są złe

Zmienna globalna to taka zadeklarowana **poza każdą funkcją i każdym komponentem**,
bezpośrednio w pliku na najwyższym poziomie.

```typescript
// Na samej górze pliku, poza wszystkim:
let licznikKlikniec = 0;  // ← ZMIENNA GLOBALNA (na poziomie modułu)

function KomponentA() {
  licznikKlikniec += 1;   // zmienia globalną
}

function KomponentB() {
  licznikKlikniec += 1;   // też zmienia tę samą globalną!
}
```

**Dlaczego globalne zmienne są problematyczne:**
- Każdy komponent, każda funkcja może je zmienić — nie wiesz kto i kiedy
- Powodują trudne do wykrycia bugi: wartość zmiennej zależy od kolejności
  wywołań, nie od danych wejściowych funkcji
- Nie przeżywają odświeżenia aplikacji (reset do wartości początkowej)
- Uniemożliwiają testy jednostkowe

**Kiedy globalne mogą być OK:** stałe konfiguracyjne które NIGDY się nie zmieniają:

```typescript
// ✅ OK — stałe (const), nigdy niezmieniane, czytelnie nazywane
const API_BASE_URL = "http://localhost:8080/api/v1";
const MAX_UPLOAD_SIZE_MB = 10;
const SUPPORTED_BREEDS = ["Labrador", "Husky"] as const;  // as const = zamroź tablicę

// ❌ ŹLE — zmienny stan jako globalna zmienna
let zalogowanyUzytkownik = null;  // do tego służy Zustand (Lekcja 12)
let listaRas = [];                 // do tego służy TanStack Query (Lekcja 11)
```

**Zasada:** stan aplikacji trzymaj w `useState` lub Zustand. Nie w globalnych zmiennych.

### Closures — funkcja która "zapamiętuje" swoje otoczenie

Closure (domknięcie) to mechanizm przez który funkcja wewnętrzna ma dostęp
do zmiennych funkcji w której została stworzona — nawet po jej zakończeniu.

```typescript
// Przykład: fabryka asów sumujących
function stworzSumator(podstawa: number) {
  // Zwracamy funkcję — ta funkcja "pamięta" wartość podstawa
  return (liczba: number) => podstawa + liczba;
}

const dodajDziesiec = stworzSumator(10);
const dodajSto     = stworzSumator(100);

console.log(dodajDziesiec(5));  // 15
console.log(dodajSto(5));       // 105
// podstawa "żyje" wewnątrz zwróconej funkcji — to jest closure
```

W React closures spotykasz codziennie — każdy handler w komponencie to closure:

```typescript
function KomponentPsa({ imie }: { imie: string }) {
  // handleKliknij "zamknął" zmienną imie ze swojego otoczenia
  const handleKliknij = () => {
    console.log(`Kliknięto psa: ${imie}`);  // imie pochodzi z zewnętrznego zakresu
  };

  return <TouchableOpacity onPress={handleKliknij}><Text>{imie}</Text></TouchableOpacity>;
}
```

---

> ### ZADANIE 1
> W `App.tsx` (nad funkcją `App`) napisz arrow function `obliczWiek` która:
> - Przyjmuje `rokUrodzenia: number`
> - Zwraca string, np. `"26 lat"` (oblicz: 2026 minus rok urodzenia)
>
> Wewnątrz komponentu `App` wyświetl wynik: `<Text>{obliczWiek(2000)}</Text>`
> Sprawdź że wyświetla się poprawna liczba lat.

---

## Lekcja 1.5 — TypeScript vs JavaScript

> Projekty w tym przewodniku są pisane wyłącznie w TypeScript.
> Ta lekcja tłumaczy na czym polega różnica i dlaczego to ważne.

### JavaScript — dynamiczne typy

JavaScript jest językiem **dynamicznie typowanym** — zmiennej możesz przypisać
cokolwiek i interpreter nie protestuje:

```javascript
// Czyste JavaScript — brak typów
let wynik = 42;
wynik = "czterdzieści dwa";   // OK — JS nie protestuje
wynik = { liczba: 42 };        // nadal OK

function dodaj(a, b) {
  return a + b;
}

dodaj(2, 3);       // 5 — OK
dodaj("2", 3);     // "23" — KONKATENACJA! nie 5! Bug!
dodaj();           // NaN — brak argumentów, JS niet narzeka
```

To powoduje całe klasy bugów koje odkrywasz dopiero w runtime — gdy aplikacja
już działa u użytkowniczki.

### TypeScript — statyczne typy

TypeScript to JavaScript z dodanym systemem typów. Kompiluje się do czystego JS
— przeglądarka ani React Native nigdy nie widzą TypeScript, tylko skompilowany JS.

```typescript
// TypeScript
let wynik: number = 42;
// wynik = "czterdzieści dwa";  // ❌ BŁĄD KOMPILACJI — już na etapie pisania

function dodaj(a: number, b: number): number {
  return a + b;
}

// dodaj("2", 3);  // ❌ BŁĄD — kompilator mówi: argument 1 musi być number
// dodaj();        // ❌ BŁĄD — brakuje argumentów
dodaj(2, 3);       // ✅ 5 — zawsze
```

**VS Code + TypeScript = natychmiastowy feedback.** Błąd pojawia się jako
czerwone podkreślenie w edytorze, zanim uruchomisz kod.

### Typy podstawowe

```typescript
// Typy prymitywne
const imie:     string  = "Burek";
const wiek:     number  = 3;        // jeden typ dla int i float
const aktywny:  boolean = true;
const nic:      null    = null;      // celowy brak wartości
const niezdef:  undefined = undefined; // wartość niezainicjalizowana

// TypeScript często SAM odgaduje typ (type inference) — nie musisz pisać
const imie2 = "Burek";   // TypeScript wie że to string — skróć zapis!
const wiek2 = 3;          // TypeScript wie że to number
```

Zapis `: string` to **adnotacja typów** — opcjonalna gdy TypeScript może sam
odgadnąć, obowiązkowa dla parametrów funkcji i zwracanych wartości.

### Union types — "to albo to"

```typescript
// Zmienna może być jednym z kilku typów
let id: string | number;        // string LUB number
id = "abc-123";                 // ✅
id = 42;                        // ✅
// id = true;                   // ❌ BŁĄD

// String literals as types — tylko konkretne wartości
type RozmiarPsa = "SMALL" | "MEDIUM" | "LARGE" | "GIANT";
let rozmiar: RozmiarPsa = "LARGE";   // ✅
// let rozmiar2: RozmiarPsa = "HUGE"; // ❌ BŁĄD — "HUGE" nie istnieje w typie

// Wartość lub null — wzorzec "coś albo nic"
let avatarUrl: string | null = null;       // użytkownik bez avatara
avatarUrl = "https://example.com/a.jpg";  // ✅
```

### Type aliases — własne nazwy typów

```typescript
// type = nadaj nazwę typowi (prostszy zapis, skrót)
type ID = string;
type Wiek = number;
type Status = "aktywny" | "nieaktywny" | "zablokowany";

// Możesz używać wszędzie zamiast powtarzania string/number
const userId: ID = "abc-123";
const status: Status = "aktywny";
```

### Interfaces — kształt obiektu

```typescript
// interface = definiujesz "kształt" obiektu (które właściwości musi mieć)
interface Pies {
  id: string;
  imie: string;
  rasa: string;
  wiek: number;
  avatarUrl: string | null;  // może być null
  czyAdopcja?: boolean;      // ? = opcjonalne (może nie istnieć)
}

// Kompilator pilnuje że obiekt ma wszystkie required pola
const burek: Pies = {
  id: "1",
  imie: "Burek",
  rasa: "Labrador",
  wiek: 3,
  avatarUrl: null,
  // czyAdopcja pominięte — OK bo jest ?
};

// burek.nieIstniejacePole;  // ❌ BŁĄD — TypeScript wie że to pole nie istnieje
```

### Generics — typy z parametrami

Generics to szablony typów — piszesz jeden raz, używasz dla wielu typów danych.

```typescript
// Array<T> — tablica elementów typu T
const rasy: Array<string> = ["Labrador", "Husky"];
const psy:  Array<Pies>   = [burek];
// Krótszy zapis:
const rasy2: string[] = ["Labrador"];
const psy2:  Pies[]   = [burek];

// Promise<T> — obietnica zwrócenia wartości typu T
async function pobierzPsa(id: string): Promise<Pies> {
  const res = await fetch(`/api/dogs/${id}`);
  return res.json();  // TypeScript wie że zwracamy Pies
}

// Własny generic — jeden interfejs dla wszystkich odpowiedzi API
interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: string;
}

type OdpowiedzPies   = ApiResponse<Pies>;          // { success, data: Pies }
type OdpowiedzLista  = ApiResponse<Pies[]>;         // { success, data: Pies[] }
type OdpowiedzString = ApiResponse<{ message: string }>;
```

### Readonly i as const — niemutowalność

```typescript
// readonly — właściwość obiektu której nie można zmienić
interface Config {
  readonly apiUrl: string;  // const dla właściwości obiektu
  timeout: number;
}
const config: Config = { apiUrl: "http://localhost:8080", timeout: 5000 };
// config.apiUrl = "coś innego";  // ❌ BŁĄD — readonly!
config.timeout = 10000;            // ✅ — to nie jest readonly

// as const — zamroź całą wartość
const KOLORY = ["czerwony", "niebieski", "zielony"] as const;
// Typ: readonly ["czerwony", "niebieski", "zielony"] — nie "string[]"
// KOLORY.push("żółty");  // ❌ BŁĄD — niemutowalna
```

### Type guards — sprawdzanie typu w runtime

```typescript
// Czasem masz union type i musisz wiedzieć KTÓRY typ masz teraz
function formatujId(id: string | number): string {
  if (typeof id === "string") {
    return id.toUpperCase();   // TypeScript wie że tu id jest string
  }
  return id.toFixed(0);        // tutaj TypeScript wie że id jest number
}

// Sprawdzanie czy obiekt to konkretny typ
interface Kot  { gatunek: "kot";  mruczy: boolean }
interface Pies { gatunek: "pies"; szczeka: boolean }
type Zwierze = Kot | Pies;

function opisZwierze(z: Zwierze): string {
  switch (z.gatunek) {
    case "kot":  return `Kot, mruczy: ${z.mruczy}`;
    case "pies": return `Pies, szczeka: ${z.szczeka}`;
  }
}
```

### `any` — ucieczka od typów (używaj TYLKO w ostateczności)

```typescript
// any = "wyłącz wszystkie kontrole typów dla tej zmiennej"
// TypeScript przestaje sprawdzać — tracisz całą wartość TS
let cokolwiek: any = 42;
cokolwiek = "string";    // OK
cokolwiek = { a: 1 };    // OK
cokolwiek.nieIstniejaca.metoda();  // ❌ CRASH w runtime — TS nie ostrzegł!

// unknown — bezpieczna alternatywa dla any
let unknown: unknown = pobierzCosSkadś();
// unknown.metoda();        // ❌ BŁĄD KOMPILACJI — musisz najpierw sprawdzić typ
if (typeof unknown === "string") {
  unknown.toUpperCase();   // ✅ — po sprawdzeniu TypeScript pozwala
}
```

**Zasada projektu:** `any` w kodzie = błąd code review. Używaj `unknown` + type guard.

### Różnice w składni — ściąga

```
JavaScript                        TypeScript
─────────────────────────────────────────────────────────────
let x = 5                         let x: number = 5
const obj = { a: 1 }              const obj: { a: number } = { a: 1 }
function f(a, b) { ... }          function f(a: string, b: number): void { ... }
array.find(x => x.id === id)      array.find((x: Pies) => x.id === id)
// brak interfejsów               interface Pies { id: string; imie: string }
// brak generyków                 Array<Pies> | Promise<ApiResponse<User>>
// brak enum / union types        type Status = "ok" | "error" | "loading"
```

Pliki `.ts` = TypeScript bez JSX | pliki `.tsx` = TypeScript z JSX (komponenty React)

---

> ### ZADANIE 1.5
> Otwórz nowy plik `typy.ts` (nie `.tsx` — brak JSX) w katalogu projektu.
> Napisz w nim:
>
> 1. Interface `Uzytkownik` z polami: `id: string`, `username: string`,
>    `email: string`, `bio: string | null`, `catchCount: number`, `isPremium?: boolean`
>
> 2. Type alias `StatusPolaczenia = "polaczono" | "rozlaczono" | "laczenie"`
>
> 3. Interface `ApiResponse<T>` z polami `success: boolean`, `data: T`,
>    `error?: string`
>
> 4. Arrow function `formatujUzytkownika(u: Uzytkownik): string` która zwraca
>    `"@username (X złapań)"` — użyj template literal
>
> 5. Zmienną `odpowiedz: ApiResponse<Uzytkownik>` z wypełnionymi danymi
>
> Sprawdź: `npx tsc --noEmit` — powinno zwrócić `0 błędów`.

---

## Lekcja 2 — Tablice i obiekty

### Tablice — 5 metod które musisz znać na pamięć

To są fundamenty Reacta. Bez nich nie napiszesz żadnej listy.

```typescript
const rasy = ["Labrador", "Husky", "Beagle", "Poodle", "Chihuahua"];

// MAP — "przetransformuj każdy element i daj mi nową tablicę"
// Najważniejsza metoda w React — używasz do RENDEROWANIA list elementów UI
const wielkie = rasy.map(rasa => rasa.toUpperCase());
// ["LABRADOR", "HUSKY", "BEAGLE", "POODLE", "CHIHUAHUA"]

const dlugosci = rasy.map(rasa => rasa.length);
// [8, 5, 6, 6, 9]

// FILTER — "daj mi tylko te elementy które spełniają warunek"
const krotkie = rasy.filter(rasa => rasa.length <= 6);
// ["Husky", "Beagle", "Poodle"]

// FIND — "znajdź mi PIERWSZY element spełniający warunek"
// Zwraca element lub undefined (może nie znaleźć!)
const znaleziona = rasy.find(rasa => rasa.includes("ea"));
// "Beagle"
const nieIstniejaca = rasy.find(rasa => rasa === "Dalmatian");
// undefined — nie ma błędu, po prostu undefined

// SOME — "czy CHOCIAŻ JEDEN element spełnia warunek?" → boolean
const czyJestHusky = rasy.some(rasa => rasa === "Husky");  // true
const czyJestDog   = rasy.some(rasa => rasa === "Bulldog"); // false

// INCLUDES — "czy tablica zawiera tę wartość?" → boolean
rasy.includes("Beagle"); // true
rasy.includes("Pug");    // false
```

**Spread operator i destrukturyzacja:**

```typescript
const pierwsze = ["Labrador", "Husky"];
const drugie   = ["Beagle", "Poodle"];

// Spread — "rozsyp zawartość tablicy"
const wszystkie = [...pierwsze, ...drugie];
// ["Labrador", "Husky", "Beagle", "Poodle"]

// Dodaj na końcu (nie mutując oryginału — ważne w React!)
const z_nowa = [...wszystkie, "Dalmatian"];

// Destrukturyzacja — wyciąganie elementów po pozycji
const [pierwsza, druga, ...reszta] = rasy;
console.log(pierwsza); // "Labrador"
console.log(reszta);   // ["Beagle", "Poodle", "Chihuahua"]
```

### Obiekty

```typescript
const pies = {
  id: "abc-123",
  imie: "Burek",
  rasa: "Labrador",
  wiek: 3,
  wlasciciel: {                    // zagnieżdżony obiekt
    imie: "Anna",
    miasto: "Warszawa"
  }
};

// Dostęp do właściwości
console.log(pies.imie);              // "Burek"
console.log(pies.wlasciciel.miasto); // "Warszawa"

// Destrukturyzacja — wyciąganie właściwości (BARDZO częste w React)
const { imie, rasa } = pies;
console.log(imie); // "Burek"

// Zmiana nazwy przy destrukturyzacji
const { imie: imiePsa, wiek: wiekPsa } = pies;

// Wartość domyślna — jeśli właściwości nie ma w obiekcie
const { avatar = "default.png" } = pies;
console.log(avatar); // "default.png" — pies nie ma tej właściwości

// Spread dla obiektów — tworzy KOPIĘ z modyfikacjami
// To jest kluczowe w React — NIGDY nie mutuj obiektów stanu bezpośrednio
const zaktualizowany = { ...pies, wiek: 4, imie: "Burek Senior" };
console.log(pies.wiek);           // 3 — oryginał bez zmian!
console.log(zaktualizowany.wiek); // 4 — nowy obiekt

// Optional chaining — bezpieczny dostęp gdy coś może być null
const uzytkownik = null;
console.log(uzytkownik?.imie);  // undefined — bez błędu, bez crasha

// Nullish coalescing — wartość domyślna gdy null lub undefined
const bio = uzytkownik?.bio ?? "Ten użytkownik nie ma opisu";
```

---

> ### ZADANIE 2
> W `App.tsx` nad komponentem `App` zdefiniuj tablicę psów:
> ```typescript
> const psy = [
>   { id: "1", imie: "Burek", rasa: "Labrador", wiek: 3, czyAdopcja: false },
>   { id: "2", imie: "Azor",  rasa: "Husky",    wiek: 7, czyAdopcja: true  },
>   { id: "3", imie: "Reks",  rasa: "Beagle",   wiek: 2, czyAdopcja: true  },
>   { id: "4", imie: "Luna",  rasa: "Poodle",   wiek: 5, czyAdopcja: false },
> ];
> ```
> Napisz i wyświetl w `<Text>`:
> 1. Imiona psów do adopcji oddzielone przecinkami (użyj `filter` + `map` + `join`)
> 2. Ile jest psów starszych niż 3 lata (użyj `filter` + `.length`)
> 3. Czy jest jakiś Labrador (użyj `some`)

---

## Lekcja 3 — Twój pierwszy komponent React Native

### Co to jest komponent?

Komponent to **funkcja która zwraca opis interfejsu** (JSX).
Wyobraź sobie LEGO — aplikacja to klocki złożone razem.
Klocek `PrzyciskDodajPsa` możesz użyć 50 razy z różnymi napisami
bez kopiowania kodu. Zmiana w jednym miejscu = zmiana wszędzie.

**Zasada:** Każdy komponent to jeden plik, jeden cel, jedna odpowiedzialność.

```tsx
// To jest komponent — zaczyna się WIELKĄ LITERĄ (OBOWIĄZKOWO)
function KartaPsa() {
  return (
    <View style={{ padding: 16, backgroundColor: "#fff", borderRadius: 12 }}>
      <Text style={{ fontSize: 18, fontWeight: "bold" }}>Burek</Text>
      <Text style={{ color: "#666" }}>Rasa: Labrador</Text>
    </View>
  );
}

// Użycie: <KartaPsa />
```

### JSX — pisanie "HTML" wewnątrz JavaScript

JSX to specjalna składnia kompilująca się do wywołań JavaScript.
Nie jest to HTML — ma inne zasady:

```tsx
function PrzykladJSX() {
  const imie = "Zuzia";
  const czyAdmin = true;
  const kolory = ["czerwony", "zielony", "niebieski"];

  return (
    <View>
      {/* Komentarz w JSX — inaczej niż // */}

      {/* {} to "wyjście do JavaScriptu" — wstawiasz wyrażenia */}
      <Text>Imię: {imie}</Text>
      <Text>Za rok będę mieć: {25 + 1} lat</Text>

      {/* Warunek TERNARY — jedyna forma if/else w JSX */}
      <Text>{czyAdmin ? "Admin" : "Użytkownik"}</Text>

      {/* Short-circuit — pokaż TYLKO gdy warunek true */}
      {czyAdmin && <Text style={{ color: "red" }}>PANEL ADMINA</Text>}

      {/* Lista przez map() — key jest WYMAGANE */}
      {kolory.map(kolor => (
        <Text key={kolor}>{kolor}</Text>
      ))}
    </View>
  );
}
```

**Czego nie wolno w JSX:**
- Nie możesz pisać `if (...) { }` wewnątrz return — tylko wyrażenia
- Wszystkie tagi muszą być zamknięte: `<Image />` nie `<Image>`
- Tekst MUSI być w `<Text>` — nie można pisać tekstu bezpośrednio w `<View>`

```tsx
// ❌ BŁĄD — crash aplikacji!
<View>
  Cześć użytkowniku
</View>

// ✅ DOBRZE
<View>
  <Text>Cześć użytkowniku</Text>
</View>
```

### 6 komponentów których potrzebujesz teraz

```tsx
import {
  View,           // kontener layout (jak <div> w HTML)
  Text,           // KAŻDY tekst musi być w <Text>
  Image,          // wyświetlanie obrazu
  TextInput,      // pole do wpisywania tekstu
  TouchableOpacity, // klikalna powierzchnia (przycisk)
  ScrollView,     // przewijana zawartość (dla małych list)
} from "react-native";

// Przykłady użycia:
<View style={{ flex: 1, padding: 16 }}>
  <Text style={{ fontSize: 20, fontWeight: "bold" }}>Tytuł</Text>

  <Image
    source={{ uri: "https://example.com/pies.jpg" }}
    style={{ width: 200, height: 200, borderRadius: 100 }}
  />

  <TextInput
    placeholder="Wpisz imię psa..."
    style={{ borderWidth: 1, padding: 12, borderRadius: 8 }}
  />

  <TouchableOpacity
    style={{ backgroundColor: "#3B82F6", padding: 16, borderRadius: 8 }}
    onPress={() => console.log("Naciśnięto!")}
  >
    <Text style={{ color: "white", textAlign: "center" }}>Kliknij mnie</Text>
  </TouchableOpacity>
</View>
```

### Flex — jak działa layout

React Native używa **Flexbox** do układania elementów. Domyślnie działa w pionie.

```tsx
// Elementy jeden pod drugim (domyślnie)
<View style={{ flex: 1 }}>
  <Text>Pierwszy</Text>
  <Text>Drugi</Text>
  <Text>Trzeci</Text>
</View>

// Elementy obok siebie
<View style={{ flexDirection: "row", alignItems: "center" }}>
  <Image style={{ width: 40, height: 40 }} source={...} />
  <Text style={{ marginLeft: 8 }}>Jan Kowalski</Text>
</View>

// Wycentruj w środku ekranu
<View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
  <Text>Jestem na środku!</Text>
</View>
```

---

> ### ZADANIE 3
> Napisz komponent `KartaUzytkownika` bezpośrednio w `App.tsx` (nad funkcją `App`).
> Komponent NIE przyjmuje żadnych props — ma na stałe wpisane dane.
>
> Wymagania:
> - Rząd: kółko z inicjałem (View + Text) | imię i email (dwa Texty w kolumnie)
> - Pod spodem: "Złapane psy: 42"
> - Złoty baner z napisem "PREMIUM" — pokaż go tylko gdy `czyPremium = true`
>   (zdefiniuj tę zmienną przed return)
> - Użyj `StyleSheet.create` dla stylów — nie wstawiaj `style={{...}}` inline
>
> Wyświetl `<KartaUzytkownika />` w `<App>` i sprawdź na telefonie.

---

## Lekcja 4 — Props: Jak komponenty rozmawiają

Komponent z Zadania 3 ma dane na stałe wpisane — jest bezużyteczny dla innych danych.
Props rozwiązują ten problem — możesz przekazać różne dane do tego samego komponentu.

```tsx
// Krok 1: Zdefiniuj interfejs props (TypeScript obowiązkowo)
interface KartaPsaProps {
  imie: string;
  rasa: string;
  wiek: number;
  imageUrl?: string;      // ? = opcjonalny
  czyAdopcja?: boolean;   // jeśli nie przekażesz, będzie undefined
}

// Krok 2: Odbierz props przez destrukturyzację
// Wartość domyślna: czyAdopcja = false gdy nie przekazano
function KartaPsa({ imie, rasa, wiek, imageUrl, czyAdopcja = false }: KartaPsaProps) {
  return (
    <View style={styles.karta}>
      {/* Opcjonalne pola — pokaż tylko gdy są */}
      {imageUrl && (
        <Image source={{ uri: imageUrl }} style={styles.obraz} />
      )}

      <Text style={styles.imie}>{imie}</Text>
      <Text style={styles.rasa}>{rasa} · {wiek} lat</Text>

      {czyAdopcja && (
        <View style={styles.badge}>
          <Text style={styles.badgeTekst}>Do adopcji</Text>
        </View>
      )}
    </View>
  );
}

// Krok 3: Użyj komponentu wielokrotnie z różnymi danymi
function App() {
  return (
    <ScrollView>
      <KartaPsa imie="Burek" rasa="Labrador" wiek={3} />
      <KartaPsa imie="Luna"  rasa="Husky"    wiek={2} czyAdopcja={true}
                imageUrl="https://images.dog.ceo/breeds/husky/n02110185_10047.jpg" />
      <KartaPsa imie="Azor"  rasa="Beagle"   wiek={7} />
    </ScrollView>
  );
}
```

### Przekazywanie funkcji przez props

Komponenty mogą też otrzymywać **funkcje** — np. "co zrobić po kliknięciu".
To jest wzorzec "wyniesienia stanu w górę" (lifting state up).

```tsx
interface PrzyciskProps {
  tytul: string;
  onPress: () => void;                                        // funkcja bez argumentów
  onDlugiePrzytrzymanie?: () => void;                        // opcjonalna
  kolor?: "niebieski" | "czerwony" | "szary";               // union type — tylko te wartości
}

function Przycisk({ tytul, onPress, onDlugiePrzytrzymanie, kolor = "niebieski" }: PrzyciskProps) {
  const kolorTla = {
    niebieski: "#3B82F6",
    czerwony:  "#EF4444",
    szary:     "#6B7280",
  }[kolor]; // dynamiczny dostęp do obiektu

  return (
    <TouchableOpacity
      style={[styles.przycisk, { backgroundColor: kolorTla }]}
      onPress={onPress}
      onLongPress={onDlugiePrzytrzymanie}
    >
      <Text style={styles.tekst}>{tytul}</Text>
    </TouchableOpacity>
  );
}

// Użycie — przekaż funkcję jako wartość props
function App() {
  return (
    <View>
      <Przycisk tytul="Zaloguj"  onPress={() => console.log("zalogowano")} />
      <Przycisk tytul="Usuń"     onPress={() => console.log("usunięto")} kolor="czerwony" />
      <Przycisk tytul="Anuluj"   onPress={() => console.log("anulowano")} kolor="szary" />
    </View>
  );
}
```

---

> ### ZADANIE 4
> Przebuduj `KartaUzytkownika` z Zadania 3 tak, żeby:
> - Przyjmowała props: `username: string`, `email: string`, `catchCount: number`,
>   `czyPremium?: boolean`, `onPress: () => void`
> - Cała karta była `TouchableOpacity` wywołującym `onPress` po kliknięciu
>
> W `App` stwórz **trzy** różne karty z różnymi danymi.
> Jedna powinna mieć `czyPremium={true}`.
> `onPress` niech wypisuje do konsoli imię klikniętego użytkownika.
> (Otwórz Metro bundler w terminalu żeby widzieć `console.log`)

---

## Lekcja 5 — useState: Komponent z pamięcią

Bez stanu komponenty zawsze wyświetlają to samo — są "martwe".
`useState` daje komponentowi **pamięć** — zapamiętuje wartość między renderowaniami.

**Co to jest renderowanie?** React wywołuje Twoją funkcję komponentu i na podstawie
zwróconego JSX aktualizuje ekran. Gdy stan się zmieni, React wywołuje funkcję ponownie.
Aktualizuje TYLKO te elementy które rzeczywiście się zmieniły — to jest fast.

```tsx
import { useState } from "react";

function Licznik() {
  // useState(wartośćStart) zwraca [aktualnaWartość, funkcjaZmiany]
  const [liczba, setLiczba] = useState(0);           // number
  const [tekst, setTekst] = useState("");            // string
  const [czyWidoczny, setCzyWidoczny] = useState(true); // boolean

  return (
    <View>
      <Text>Aktualnie: {liczba}</Text>

      {/* Setter z bezpośrednią wartością */}
      <TouchableOpacity onPress={() => setLiczba(liczba + 1)}>
        <Text>+1</Text>
      </TouchableOpacity>

      {/* Setter z "funkcją aktualizującą" — UŻYWAJ TEGO gdy nowa wartość zależy od starej */}
      <TouchableOpacity onPress={() => setLiczba(prev => prev - 1)}>
        <Text>-1</Text>
      </TouchableOpacity>

      <TouchableOpacity onPress={() => setLiczba(0)}>
        <Text>Reset</Text>
      </TouchableOpacity>

      {/* TextInput ze stanem */}
      <TextInput
        value={tekst}
        onChangeText={setTekst}     // wywołuje setTekst(nowy_tekst) przy każdej literze
        placeholder="Wpisz coś..."
        style={{ borderWidth: 1, padding: 12 }}
      />

      {/* Toggle */}
      <TouchableOpacity onPress={() => setCzyWidoczny(prev => !prev)}>
        <Text>{czyWidoczny ? "Ukryj" : "Pokaż"}</Text>
      </TouchableOpacity>
      {czyWidoczny && <Text>Tajemna treść!</Text>}
    </View>
  );
}
```

### Złota zasada: NIGDY nie mutuj stanu bezpośrednio

```typescript
// ❌ ŹLE — React nie widzi zmiany, ekran SIĘ NIE odświeży
const [lista, setLista] = useState(["Labrador", "Husky"]);
lista.push("Beagle");           // mutacja oryginału — BUG

const [pies, setPies] = useState({ imie: "Burek", wiek: 3 });
pies.wiek = 4;                  // mutacja oryginału — BUG

// ✅ DOBRZE — tworzysz NOWY obiekt/tablicę przez spread
setLista(prev => [...prev, "Beagle"]);
setPies(prev => ({ ...prev, wiek: 4 }));
```

### Stan dla formularzy

```tsx
interface FormLogin {
  email: string;
  haslo: string;
  czyPokazHaslo: boolean;
}

function EkranLogowania() {
  const [form, setForm] = useState<FormLogin>({
    email: "",
    haslo: "",
    czyPokazHaslo: false,
  });

  // Jeden handler dla wszystkich pól — keyof FormLogin to typ: "email" | "haslo" | "czyPokazHaslo"
  const zmienPole = (pole: keyof FormLogin, wartosc: string | boolean) => {
    setForm(prev => ({ ...prev, [pole]: wartosc }));
    //                           ↑ computed property — nazwa klucza z zmiennej
  };

  return (
    <View style={{ padding: 24, gap: 12 }}>
      <TextInput
        value={form.email}
        onChangeText={tekst => zmienPole("email", tekst)}
        placeholder="Email"
        keyboardType="email-address"
        autoCapitalize="none"
        style={{ borderWidth: 1, padding: 12, borderRadius: 8 }}
      />

      <TextInput
        value={form.haslo}
        onChangeText={tekst => zmienPole("haslo", tekst)}
        placeholder="Hasło"
        secureTextEntry={!form.czyPokazHaslo}
        style={{ borderWidth: 1, padding: 12, borderRadius: 8 }}
      />

      <TouchableOpacity onPress={() => zmienPole("czyPokazHaslo", !form.czyPokazHaslo)}>
        <Text>{form.czyPokazHaslo ? "Ukryj" : "Pokaż"} hasło</Text>
      </TouchableOpacity>

      <TouchableOpacity
        style={{ backgroundColor: "#3B82F6", padding: 16, borderRadius: 8 }}
        onPress={() => console.log("Logowanie:", form.email)}
      >
        <Text style={{ color: "white", textAlign: "center", fontWeight: "bold" }}>
          Zaloguj się
        </Text>
      </TouchableOpacity>
    </View>
  );
}
```

---

> ### ZADANIE 5
> Napisz komponent `LicznikPunktow`:
> - Wyświetla aktualny wynik (zaczyna od 0)
> - Przycisk "+10" — dodaje 10 punktów
> - Przycisk "-5" — odejmuje 5 (wynik NIGDY nie może być ujemny — sprawdź warunkiem)
> - Przycisk "Reset" — wraca do 0
> - Przycisk "Podwój!" — mnoży wynik przez 2
> - Gdy wynik > 100 → tekst zmienia kolor na złoty i wyświetla "SUPER WYNIK!"
> - Gdy wynik === 0 → wyświetla szary tekst "Zacznij grać!"
>
> **Bonus:** Dodaj historię ostatnich 5 operacji jako tablicę stringów
> (np. `["Dodano +10", "Odjęto -5", "Reset do 0"]`).
> Wyświetl ją pod licznikiem. Używaj `setHistoria(prev => [nowy, ...prev].slice(0, 5))`.

---

## Lekcja 6 — FlatList: Wyświetlanie list

`ScrollView` renderuje WSZYSTKICH dzieci naraz. Przy 10 000 elementów = zamrożona aplikacja.

`FlatList` renderuje tylko elementy **widoczne na ekranie**. Reszta jest wirtualizowana.
**Zasada:** dla każdej listy danych zawsze `FlatList`, nie `map()` w `ScrollView`.

```tsx
import { FlatList, View, Text, TouchableOpacity, TextInput, ActivityIndicator } from "react-native";
import { useState } from "react";

interface Rasa {
  id: string;
  nazwa: string;
  grupa: string;
  rzadkoscScore: number;
}

const RASY: Rasa[] = [
  { id: "1", nazwa: "Labrador",   grupa: "Retrievery", rzadkoscScore: 1 },
  { id: "2", nazwa: "Husky",      grupa: "Sanie",      rzadkoscScore: 2 },
  { id: "3", nazwa: "Beagle",     grupa: "Gończe",     rzadkoscScore: 2 },
  { id: "4", nazwa: "Poodle",     grupa: "Towiańskie", rzadkoscScore: 1 },
  { id: "5", nazwa: "Chihuahua",  grupa: "Towarzyskie",rzadkoscScore: 3 },
];

// Dobra praktyka: wyciągnij item do osobnego komponentu
function PozycjaRasy({ rasa, onPress }: { rasa: Rasa; onPress: () => void }) {
  return (
    <TouchableOpacity
      style={{ padding: 16, borderBottomWidth: 1, borderColor: "#eee" }}
      onPress={onPress}
    >
      <Text style={{ fontSize: 16, fontWeight: "bold" }}>{rasa.nazwa}</Text>
      <Text style={{ color: "#666" }}>{rasa.grupa}</Text>
      <Text>{"⭐".repeat(rasa.rzadkoscScore)}</Text>
    </TouchableOpacity>
  );
}

function ListaRas() {
  const [wyszukiwanie, setWyszukiwanie] = useState("");

  const przefiltrowane = RASY.filter(rasa =>
    rasa.nazwa.toLowerCase().includes(wyszukiwanie.toLowerCase())
  );

  return (
    <View style={{ flex: 1 }}>
      <TextInput
        value={wyszukiwanie}
        onChangeText={setWyszukiwanie}
        placeholder="Szukaj rasy..."
        style={{ margin: 16, padding: 12, borderWidth: 1, borderRadius: 8 }}
      />

      <FlatList
        data={przefiltrowane}
        keyExtractor={item => item.id}    // ZAWSZE używaj unikalnego ID, nie indeksu!
        renderItem={({ item }) => (
          <PozycjaRasy
            rasa={item}
            onPress={() => console.log(`Wybrano: ${item.nazwa}`)}
          />
        )}

        // Nagłówek
        ListHeaderComponent={() => (
          <Text style={{ padding: 16, fontWeight: "bold", fontSize: 18 }}>
            Wszystkie rasy ({przefiltrowane.length})
          </Text>
        )}

        // Pusta lista
        ListEmptyComponent={() => (
          <View style={{ padding: 32, alignItems: "center" }}>
            <Text style={{ color: "#999" }}>
              Nie znaleziono rasy "{wyszukiwanie}"
            </Text>
          </View>
        )}

        // Pull-to-refresh
        refreshing={false}
        onRefresh={() => console.log("Odświeżam list...")}
      />
    </View>
  );
}
```

**Dlaczego `keyExtractor` z `.id`, nie z indeksu?**
Jeśli posortują się elementy lub usuniemy jeden ze środka, React na podstawie klucza
wie KTÓRY element się zmienił i musi odświeżyć. Z indeksami pomyliłby elementy
i robił niepotrzebne re-rendery (lub co gorsza — mylił dane).

---

> ### ZADANIE 6
> Napisz komponent `ListaAdopcji` który:
> - Używa tablicy psów z Zadania 2 jako stan (`useState`)
> - Wyświetla `FlatList` psów
> - Każda pozycja: imię, rasa, wiek + przycisk "Adoptuj"
> - Kliknięcie "Adoptuj" usuwa psa z listy (`filter`)
> - `ListEmptyComponent`: "Wszystkie psy znalazły dom! 🐾"
>   (emoji dozwolone w treści widocznej dla użytkownika)
> - Pole `TextInput` filtruje po imieniu lub rasie
>
> **Bonus:** Przycisk "Cofnij" wraca do stanu sprzed ostatniej adopcji.
> Trzymaj poprzednią tablicę w osobnym `useState<typeof psy | null>(null)`.

---

## Lekcja 7 — useEffect: "Uruchom to po załadowaniu"

Czasem chcesz coś wykonać PO wyrenderowaniu komponentu — pobrać dane, uruchomić timer,
subskrybować zdarzenia. `useEffect` to do tego służy.

```tsx
import { useState, useEffect } from "react";

function PrzykladyEffect() {
  const [sekunda, setSekunda] = useState(0);
  const [userId, setUserId] = useState("user-123");

  // Forma 1: [] — uruchom RAZ po pierwszym pojawieniu się na ekranie
  useEffect(() => {
    console.log("Komponent pojawił się na ekranie — pobierz dane!");
  }, []);

  // Forma 2: [zależność] — uruchom gdy zmienia się userId
  useEffect(() => {
    console.log(`UserId zmienił się na: ${userId} — pobierz nowy profil!`);
  }, [userId]);

  // Forma 3: cleanup — sprzątaj po sobie (zatrzymaj timer, anuluj request)
  useEffect(() => {
    const interval = setInterval(() => {
      setSekunda(prev => prev + 1);
    }, 1000);

    // Zwrócona funkcja wywoła się gdy komponent ZNIKNIE z ekranu
    return () => {
      clearInterval(interval);
      console.log("Komponent zniknął — timer zatrzymany");
    };
  }, []); // [] = uruchom interval raz, cleanup wykona się przy odmontowaniu

  return <Text>Upłynęło: {sekunda} sekund</Text>;
}
```

**Tablica zależności — zapamiętaj:**

```
[]           → Raz po załadowaniu (jak "on mount")
[a, b]       → Przy pierwszym załadowaniu ORAZ gdy zmieni się a lub b
brak tablicy → Po KAŻDYM renderowaniu (prawie nigdy nie chcesz tego)
return fn    → Cleanup: gdy komponent zniknie lub zanim odpalasz nowy effect
```

---

> ### ZADANIE 7
> Napisz komponent `ZegarCyfrowy`:
> - Wyświetla aktualny czas w formacie HH:MM:SS
> - Aktualizuje się co sekundę
> - Pamiętaj o cleanup — zatrzymaj interval
> - Wskazówka: `new Date().toLocaleTimeString("pl-PL")`
>
> **Bonus:** Przyciski "Zatrzymaj" i "Wznów" — kontroluj `czyDziala: boolean` stanem.
> Zegar powinien zatrzymać się przy kliknięciu "Zatrzymaj" i ruszyć po "Wznów".

---

## Lekcja 8 — async/await: Rozmowa z serwerem

Serwer nie odpowiada natychmiast — zapytanie leci przez internet, serwer przetwarza,
odpowiedź wraca. To zajmuje czas. Twój kod musi obsłużyć ten czas oczekiwania
bez zamrożenia całej aplikacji.

```typescript
// Promise — "obietnica" dostarczenia wartości w przyszłości
// Ma 3 stany: pending (czekamy) | fulfilled (dostaliśmy) | rejected (błąd)

// async/await — czystszy zapis pracy z Promises
async function pobierzDane(url: string) {
  try {
    // await = "poczekaj na wynik ale nie blokuj całej aplikacji"
    const response = await fetch(url);

    // Zawsze sprawdzaj status HTTP!
    if (!response.ok) {
      throw new Error(`Błąd serwera: ${response.status}`);
    }

    // Parsowanie JSON też jest async
    const data = await response.json();
    return data;

  } catch (error) {
    // Ten catch łapie: błąd sieci + błąd serwera + błąd parsowania JSON
    console.error("Coś poszło nie tak:", error);
    throw error; // przekaż błąd wyżej jeśli potrzebujesz
  }
}
```

### Wzorzec: pobieranie danych w useEffect (naucz się NA PAMIĘĆ)

Ten wzorzec będziesz pisać setki razy. Opanuj go teraz.

```tsx
import { useState, useEffect } from "react";
import { View, Text, ActivityIndicator, TouchableOpacity } from "react-native";

function EkranRasy({ rasaId }: { rasaId: string }) {
  const [rasa, setRasa]       = useState<Rasa | null>(null);
  const [ladowanie, setLadowanie] = useState(true);
  const [blad, setBlad]       = useState<string | null>(null);

  useEffect(() => {
    let aktywny = true; // zabezpieczenie przed aktualizacją odmontowanego komponentu

    async function pobierz() {
      try {
        setLadowanie(true);
        setBlad(null);

        const response = await fetch(`https://api.example.com/breeds/${rasaId}`);
        if (!response.ok) throw new Error(`Błąd: ${response.status}`);
        const data = await response.json();

        if (aktywny) setRasa(data); // sprawdź czy komponent nadal jest na ekranie

      } catch (err) {
        if (aktywny) setBlad(err instanceof Error ? err.message : "Nieznany błąd");
      } finally {
        if (aktywny) setLadowanie(false);
      }
    }

    pobierz();
    return () => { aktywny = false; };

  }, [rasaId]); // pobierz ponownie gdy rasaId się zmieni

  // ZAWSZE obsługuj wszystkie trzy stany — nigdy nie pomijaj
  if (ladowanie) return <ActivityIndicator size="large" color="#3B82F6" />;

  if (blad) return (
    <View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
      <Text style={{ color: "red", marginBottom: 12 }}>Ups! {blad}</Text>
      <TouchableOpacity onPress={() => setBlad(null)}>
        <Text style={{ color: "#3B82F6" }}>Spróbuj ponownie</Text>
      </TouchableOpacity>
    </View>
  );

  if (!rasa) return null;

  return (
    <View>
      <Text style={{ fontSize: 24, fontWeight: "bold" }}>{rasa.nazwa}</Text>
    </View>
  );
}
```

---

> ### ZADANIE 8
> Użyj publicznego API psów — nie wymaga klucza:
> `https://dog.ceo/api/breeds/list/all`
>
> Napisz komponent `ApiListaRas` który:
> - Pobiera listę ras przy załadowaniu
> - Wyświetla `ActivityIndicator` podczas ładowania
> - Wyświetla błąd z przyciskiem "Spróbuj ponownie" gdy coś pójdzie nie tak
> - Wyświetla `FlatList` z nazwami ras (API zwraca obiekt — użyj `Object.keys(data.message)`)
> - Ma przycisk "Odśwież" który pobiera dane ponownie
>   (dodaj stan `const [klucz, setKlucz] = useState(0)` i daj go do zależności useEffect,
>   przycisk robi `setKlucz(prev => prev + 1)`)
>
> **Sprawdź:** wyłącz Wi-Fi na chwilę, odśwież — powinna pojawić się obsługa błędu.

---

## Lekcja 9 — Struktura projektu: Najważniejsza lekcja

> Możesz napisać całą aplikację w jednym pliku `App.tsx`.
> Możesz też zbudować dom stawiając wszystkie cegły w jedną ścianę.
> Oba działają — żadnego z nich nie chcesz.

### Problem z jednym plikiem — konkretny scenariusz

Wyobraź sobie `App.tsx` z 1500 linii:

```
linijki 1-200:    logika formularza logowania + walidacja
linijki 200-400:  fetch psów z API + caching
linijki 400-600:  komponent karty użytkownika + style
linijki 600-900:  FlatList feedu + infinite scroll
linijki 900-1200: konfiguracja nawigacji
linijki 1200-1500: style dla 15 różnych komponentów
```

Twoja koleżanka chce zmienić kolor przycisku w formularzu logowania.
Szuka 8 minut. Zmienia `#3B82F6` na `#6366F1`. Przypadkowo edytuje linię obok —
funkcję walidacji emaila. Commit. CI czerwone. Środa zrujnowana.

### Zasada: jeden plik = jedna odpowiedzialność

Każdy plik powinien robić **jedną rzecz** i tylko jedną.

### Struktura projektu — wyucz się na pamięć

```
TwojApka/
│
├── App.tsx                  ← TYLKO konfiguracja root (providers, nawigacja)
│                               Docelowo: < 30 linii
│
├── screens/                 ← EKRANY — całe widoki po przejściu nawigacją
│   ├── HomeScreen.tsx
│   ├── ListaPsowScreen.tsx
│   └── SzczegolyPsaScreen.tsx
│
├── components/              ← KOMPONENTY UI — reużywalne klocki
│   ├── ui/                  ← bazowe: Button, Input, Card, Avatar (używane wszędzie)
│   │   ├── Button.tsx
│   │   └── Input.tsx
│   └── psy/                 ← składowe dla funkcji "psy"
│       ├── KartaPsa.tsx
│       └── RzadkoscBadge.tsx
│
├── hooks/                   ← HOOKI — logika wyciągnięta z komponentów
│   └── usePsy.ts            ← zarządza stanem: lista psów, ladowanie, blad
│
├── services/                ← SERWISY — JEDYNE miejsce gdzie jest fetch
│   └── psyService.ts        ← funkcje: pobierzPsy(), dodajPsa(), usunPsa()
│
├── types/                   ← TYPY TypeScript — jeden punkt prawdy
│   └── index.ts             ← interface Pies, interface Uzytkownik, ...
│
└── constants/               ← STAŁE — URL-e, kolory, wartości konfiguracyjne
    └── config.ts            ← API_BASE_URL (nigdy nie hardkoduj URL w kodzie!)
```

### Przepisywanie krok po kroku

Zacznijmy od "bałaganu" — całość w App.tsx:

```tsx
// App.tsx — ZACZYNAMY TUTAJ (źle, ale działa)

export default function App() {
  const [psy, setPsy] = useState<any[]>([]);
  const [ladowanie, setLadowanie] = useState(true);

  useEffect(() => {
    fetch("https://dog.ceo/api/breeds/list/all")
      .then(r => r.json())
      .then(data => {
        setPsy(Object.keys(data.message));
        setLadowanie(false);
      });
  }, []);

  return (
    <SafeAreaView style={{ flex: 1 }}>
      {ladowanie ? (
        <ActivityIndicator style={{ flex: 1 }} />
      ) : (
        <FlatList
          data={psy}
          keyExtractor={(item, i) => String(i)} // ← ŹLE używamy indeksu
          renderItem={({ item }) => (
            <TouchableOpacity style={{ padding: 16, borderBottomWidth: 1 }}
                              onPress={() => console.log(item)}>
              <Text style={{ fontSize: 16 }}>{item}</Text>
            </TouchableOpacity>
          )}
        />
      )}
    </SafeAreaView>
  );
}
```

#### Krok 1: Typy do `types/index.ts`

```typescript
// types/index.ts
// Jeden plik, wszystkie typy — importujesz stąd w całym projekcie

export interface Rasa {
  id: string;   // będziemy generować z nazwy
  nazwa: string;
}
```

#### Krok 2: Serwis do `services/rasaService.ts`

```typescript
// services/rasaService.ts
import { Rasa } from "../types";

// Jedyne miejsce w projekcie z fetch — zero useState, zero JSX tutaj
export async function pobierzRasy(): Promise<Rasa[]> {
  const response = await fetch("https://dog.ceo/api/breeds/list/all");
  if (!response.ok) throw new Error(`Błąd: ${response.status}`);
  const data = await response.json();

  // Transformuj odpowiedź API do naszego formatu
  return Object.keys(data.message).map(nazwa => ({
    id: nazwa,           // nazwa jako ID — teraz unikalne i stabilne
    nazwa: nazwa,
  }));
}
```

#### Krok 3: Hook do `hooks/useRasy.ts`

```typescript
// hooks/useRasy.ts
import { useState, useEffect } from "react";
import { Rasa } from "../types";
import { pobierzRasy } from "../services/rasaService";

// Hook nie wie skąd dane — deleguje do serwisu
// Hook nie wie jak wyświetlić dane — deleguje do komponentu
// Hook zarządza TYLKO stanem i logiką biznesową
export function useRasy() {
  const [rasy, setRasy]         = useState<Rasa[]>([]);
  const [ladowanie, setLadowanie] = useState(true);
  const [blad, setBlad]         = useState<string | null>(null);

  const odswiez = async () => {
    try {
      setLadowanie(true);
      setBlad(null);
      const dane = await pobierzRasy();
      setRasy(dane);
    } catch (err) {
      setBlad(err instanceof Error ? err.message : "Błąd pobierania");
    } finally {
      setLadowanie(false);
    }
  };

  useEffect(() => { odswiez(); }, []);

  return { rasy, ladowanie, blad, odswiez };
}
```

#### Krok 4: Komponent do `components/rasy/KartaRasy.tsx`

```tsx
// components/rasy/KartaRasy.tsx
import { TouchableOpacity, Text, StyleSheet } from "react-native";
import { Rasa } from "../../types";

interface KartaRasyProps {
  rasa: Rasa;
  onPress: (rasa: Rasa) => void;
}

function KartaRasy({ rasa, onPress }: KartaRasyProps) {
  return (
    <TouchableOpacity style={styles.karta} onPress={() => onPress(rasa)}>
      <Text style={styles.nazwa}>{rasa.nazwa}</Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  karta: { padding: 16, borderBottomWidth: 1, borderColor: "#eee" },
  nazwa: { fontSize: 16, textTransform: "capitalize" },
});

export default KartaRasy;
// ↑ Jeden plik. Jeden komponent. 20 linii. Łatwy do znalezienia, edytowania, testowania.
```

#### Krok 5: Ekran do `screens/ListaRasScreen.tsx`

```tsx
// screens/ListaRasScreen.tsx
import { View, FlatList, ActivityIndicator, Text, TouchableOpacity } from "react-native";
import { useRasy } from "../hooks/useRasy";
import KartaRasy from "../components/rasy/KartaRasy";
import { Rasa } from "../types";

function ListaRasScreen() {
  const { rasy, ladowanie, blad, odswiez } = useRasy();

  if (ladowanie) return <ActivityIndicator style={{ flex: 1 }} size="large" />;
  if (blad) return (
    <View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
      <Text style={{ color: "red", marginBottom: 12 }}>{blad}</Text>
      <TouchableOpacity onPress={odswiez}><Text>Spróbuj ponownie</Text></TouchableOpacity>
    </View>
  );

  const handleWybierz = (rasa: Rasa) => {
    console.log("Wybrano:", rasa.nazwa);
  };

  return (
    <View style={{ flex: 1 }}>
      <FlatList
        data={rasy}
        keyExtractor={item => item.id}  // teraz używamy .id — dobrze!
        renderItem={({ item }) => <KartaRasy rasa={item} onPress={handleWybierz} />}
        onRefresh={odswiez}
        refreshing={ladowanie}
      />
    </View>
  );
}

export default ListaRasScreen;
// Ekran wie jak ułożyć dane — nie wie skąd dane (deleguje do hooka)
// Nie wie jak wygląda karta — deleguje do KartaRasy
```

#### Krok 6: App.tsx stał się prosty

```tsx
// App.tsx — teraz tylko konfiguracja
import { SafeAreaView } from "react-native-safe-area-context";
import ListaRasScreen from "./screens/ListaRasScreen";

export default function App() {
  return (
    <SafeAreaView style={{ flex: 1 }}>
      <ListaRasScreen />
    </SafeAreaView>
  );
}
// 8 linii. Czytelne. Ewidentne że tu zaczyna się aplikacja.
```

### Przepływ danych — zapamiętaj ten rzut

```
services/   →  hooks/     →  screens/   →  components/
  pobierz       zarządzaj    ułóż          wyświetl
  dane          stanem       layout        jeden element
  z API         i błędami    ekranu        UI
```

Każda warstwa zna tylko sąsiednią. `KartaRasy` nie wie że dane przyszły z internetu.
`ListaRasScreen` nie wie jak wygląda karta. `useRasy` nie wie co jest wyświetlane.
To jest **separacja odpowiedzialności** — klucz do kodu który da się utrzymywać.

---

> ### ZADANIE 9 — Refaktoryzacja
> Wróć do swojej aplikacji TodoList z Zadań 1-8 (lub napisz ją od zera).
> Przestrukturyzuj ją zgodnie z powyższymi zasadami.
>
> Wymagana struktura:
> ```
> TodoApp/
> ├── App.tsx                    ← max 15 linii
> ├── screens/
> │   └── TodoScreen.tsx
> ├── components/
> │   ├── ui/
> │   │   └── Button.tsx         ← props: tytul, onPress, kolor?
> │   └── todo/
> │       ├── TodoItem.tsx       ← props: todo, onToggle, onDelete
> │       └── TodoFiltry.tsx     ← props: aktywny, onChange
> ├── hooks/
> │   └── useTodo.ts             ← cały stan: lista, dodaj, usun, toggle, filtr
> └── types/
>     └── index.ts               ← interface Todo { id, tytul, ukonczone, dataWykonania? }
> ```
>
> Sprawdzenie: otwórz `App.tsx` — powinna mieć max 15 linii.
> Otwórz `Button.tsx` — powinna być używana w co najmniej 2 różnych miejscach.

---

## Projekt 1 — Tamagotchi App

Czas złożyć wszystko razem. Budujesz wirtualnego psa z prawdziwą logiką.

### Koncepcja

Twój wirtualny pies ma trzy wskaźniki: `glod` (0-100), `szczescie` (0-100),
`energia` (0-100). Wskaźniki opadają co kilka sekund. Karmisz go, bawisz się
z nim, kładziesz spać. Jeśli jakikolwiek wskaźnik spadnie do 0 — pies "umiera".

### Wymagana struktura

```
TamagotchiApp/
├── App.tsx                          ← tylko providers + root screen
├── screens/
│   └── TamagotchiScreen.tsx         ← główny ekran z bud psa i akcjami
├── components/
│   ├── ui/
│   │   └── PasekWskaznika.tsx       ← props: wartosc (0-100), kolor, tytul
│   └── tamagotchi/
│       ├── TwarzPsa.tsx             ← wyświetla emoji na podstawie stanu
│       └── PasekAkcji.tsx           ← trzy przyciski akcji
├── hooks/
│   └── useTamagotchi.ts             ← CAŁY stan i logika
└── types/
    └── index.ts                     ← interface StanPsa
```

### Specyfikacja `useTamagotchi.ts`

```typescript
interface StanPsa {
  imie: string;
  glod: number;        // 0-100 (100 = najedzony, 0 = głodny)
  szczescie: number;   // 0-100
  energia: number;     // 0-100
  czyZyje: boolean;
  wiek: number;        // sekundy życia
}

// Co 3 sekundy (useEffect + setInterval):
//   glod     -= 5
//   szczescie -= 3
//   energia  -= 2
//   wiek     += 1
//   Jeśli któryś < 0 → ustaw na 0, jeśli wszystkie 0 → czyZyje = false

// Akcja karm(): glod = Math.min(100, glod + 30), energia -= 5
// Akcja baw():  szczescie = Math.min(100, szczescie + 25), energia -= 15
// Akcja spij(): energia = Math.min(100, energia + 40), szczescie -= 10

// resetuj(): przywraca stan początkowy (nowe życie)
```

### Specyfikacja `TwarzPsa.tsx`

Wyświetla jeden duży emoji na podstawie stanu:
- `czyZyje = false` → "💀"
- `glod < 20` → "😫" (głodny)
- `szczescie < 20` → "😢" (smutny)
- `energia < 20` → "😴" (śpiący)
- Wszystko OK → "🐶"

### Kryteria zaliczenia

- Wskaźniki opadają z czasem i widać to na pasku
- Przyciski zmieniają wskaźniki poprawnie
- Przycisk "Baw się" jest `disabled` gdy `energia < 20` (pies zmęczony)
- Po śmierci pojawiają się "💀" i szara plansza z napisem "RIP" + czas życia
- Przycisk "Zacznij od nowa" działa po śmierci
- `App.tsx` ma max 15 linii
- `useTamagotchi.ts` nie zawiera žadnego JSX — tylko logikę
- Zero `any` w TypeScript

---

# ETAP 2 — Wzorce produkcyjne

---

## Lekcja 10 — Expo Router: Nawigacja przez pliki

W Etapie 1 miałaś jedną aplikację z jednym ekranem. Prawdziwa aplikacja ma dziesiątki
ekranów: logowanie, feed, profil, szczegóły, ustawienia...

Expo Router to system nawigacji oparty na **strukturze plików** — tak jak Next.js.
Plik w katalogu `app/` automatycznie staje się ekranem. Nie musisz nic rejestrować.

### Instalacja projektu z Expo Router

```bash
npx create-expo-app NawigacjaApka --template tabs
cd NawigacjaApka
npm start
```

### Jak działają pliki = trasy

```
app/
├── _layout.tsx          ← konfiguracja wszystkich tras (providers, header)
├── index.tsx            ← trasa "/"  (ekran startowy)
├── login.tsx            ← trasa "/login"
├── (auth)/              ← GRUPA — nie tworzy segmentu URL, tylko organizuje pliki
│   ├── register.tsx     ← trasa "/register"  (nie "/auth/register"!)
│   └── verify.tsx       ← trasa "/verify"
├── (tabs)/              ← GRUPA z tab bar — zakładki na dole
│   ├── _layout.tsx      ← konfiguracja tab baru
│   ├── feed.tsx         ← zakładka "Feed"
│   └── profil.tsx       ← zakładka "Profil"
└── pies/
    └── [id].tsx         ← "/pies/abc123" — [id] to parametr dynamiczny
```

### Nawigacja programowa

```tsx
// app/(tabs)/feed.tsx
import { View, Text, TouchableOpacity } from "react-native";
import { router, Link } from "expo-router";
import { Stack } from "expo-router";

export default function FeedEkran() {
  const idPsa = "abc-123";

  return (
    <View style={{ flex: 1 }}>
      {/* Stack.Screen — konfiguruj nagłówek z wnętrza ekranu */}
      <Stack.Screen options={{ title: "Mój Feed" }} />

      {/* Link — deklaratywna nawigacja (jak <a> w HTML) */}
      <Link href="/login">Idź do logowania</Link>
      <Link href={`/pies/${idPsa}`}>Otwórz szczegóły</Link>

      {/* router.push — programowa nawigacja */}
      <TouchableOpacity onPress={() => router.push("/login")}>
        <Text>Zaloguj się</Text>
      </TouchableOpacity>

      {/* replace — nie dodawaj do historii (użytkownik nie może "wróć") */}
      <TouchableOpacity onPress={() => router.replace("/(tabs)/feed")}>
        <Text>Do feedu bez powrotu</Text>
      </TouchableOpacity>

      <TouchableOpacity onPress={() => router.back()}>
        <Text>Wróć</Text>
      </TouchableOpacity>
    </View>
  );
}
```

```tsx
// app/pies/[id].tsx — odbieranie parametru dynamicznego
import { useLocalSearchParams } from "expo-router";

export default function SzczegolyPsaEkran() {
  const { id } = useLocalSearchParams<{ id: string }>();    // TypeScript dla params
  return <Text>Szczegóły psa o ID: {id}</Text>;
}
```

```tsx
// app/(tabs)/_layout.tsx — konfiguracja zakładek
import { Tabs } from "expo-router";
import { Ionicons } from "@expo/vector-icons";

export default function TabsLayout() {
  return (
    <Tabs screenOptions={{ tabBarActiveTintColor: "#3B82F6" }}>
      <Tabs.Screen
        name="feed"
        options={{
          title: "Feed",
          tabBarIcon: ({ color }) => <Ionicons name="home" size={24} color={color} />,
        }}
      />
      <Tabs.Screen
        name="profil"
        options={{
          title: "Profil",
          tabBarIcon: ({ color }) => <Ionicons name="person" size={24} color={color} />,
        }}
      />
    </Tabs>
  );
}
```

---

> ### ZADANIE 10
> Stwórz aplikację z nawigacją na bazie listy psów:
> - Zakładka "Psy" — FlatList psów (statyczne dane, min 8 psów)
> - Zakładka "Ulubione" — początkowo pusta
> - Kliknięcie w psa → ekran `/pies/[id]` ze szczegółami i przyciskiem "Dodaj do ulubionych"
> - Po kliknięciu "Dodaj do ulubionych" pies pojawia się na zakładce "Ulubione"
>
> Wskazówka: trzymaj ulubione w module-level zmiennej (poza komponentem) albo
> użyj Zustand z następnej lekcji.

---

## Lekcja 11 — TanStack Query: Profesjonalna komunikacja z API

W Lekcji 8 pisałaś `useEffect + useState` dla każdego endpointu.
To działa, ale brakuje: cache, auto-retry, synchronizacji między ekranami.

**TanStack Query** rozwiązuje to wszystko:

```bash
npm install @tanstack/react-query
```

```tsx
// app/_layout.tsx — raz w aplikacji
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // dane "świeże" 5 minut — bez ponownego fetcha
      retry: 2,
    },
  },
});

export default function RootLayout() {
  return (
    <QueryClientProvider client={queryClient}>
      <Stack />
    </QueryClientProvider>
  );
}
```

### useQuery — zastępuje useEffect + useState

```typescript
// hooks/useRasy.ts
import { useQuery } from "@tanstack/react-query";
import { pobierzRasy } from "../services/rasaService";

// Centralne klucze — ważne dla invalidacji cache
export const KLUCZE = {
  rasy: {
    lista: (q?: string) => ["rasy", "lista", q] as const,
    szczegoly: (id: string) => ["rasy", "szczegoly", id] as const,
  },
};

export function useRasy(wyszukiwanie?: string) {
  return useQuery({
    queryKey: KLUCZE.rasy.lista(wyszukiwanie),
    queryFn: () => pobierzRasy(wyszukiwanie),
  });
}
```

```tsx
// Użycie w komponencie — o wiele krótsze niż useEffect!
function ListaRas() {
  const { data: rasy, isLoading, isError, error, refetch } = useRasy();

  if (isLoading) return <ActivityIndicator />;
  if (isError)   return <Text>Błąd: {error.message}</Text>;

  return (
    <FlatList
      data={rasy}
      keyExtractor={r => r.id}
      renderItem={({ item }) => <KartaRasy rasa={item} />}
      onRefresh={refetch}  // pull-to-refresh automatycznie przez refetch
      refreshing={isLoading}
    />
  );
}
```

### useMutation + Optimistic Update — lajkowanie

```typescript
// hooks/useLike.ts
import { useMutation, useQueryClient } from "@tanstack/react-query";

export function useLike(catchId: string) {
  const queryClient = useQueryClient();
  const klucz = ["catch", catchId];

  return useMutation({
    mutationFn: (czyLubioneTeras: boolean) =>
      czyLubioneTeras ? usunLike(catchId) : dodajLike(catchId),

    // onMutate = uruchom PRZED requestem do serwera
    onMutate: async (czyLubioneTeras) => {
      await queryClient.cancelQueries({ queryKey: klucz });
      const poprzedni = queryClient.getQueryData(klucz);

      // Zaktualizuj UI natychmiast — zanim serwer potwierdzi
      queryClient.setQueryData(klucz, (stary: any) => ({
        ...stary,
        likeCount: czyLubioneTeras ? stary.likeCount - 1 : stary.likeCount + 1,
        likedByCurrentUser: !czyLubioneTeras,
      }));

      return { poprzedni };
    },

    // Rollback jeśli serwer zwrócił błąd
    onError: (_, __, context) => {
      queryClient.setQueryData(klucz, context?.poprzedni);
    },

    // Odśwież po zakończeniu (niezależnie od wyniku)
    onSettled: () => queryClient.invalidateQueries({ queryKey: klucz }),
  });
}
```

### useInfiniteQuery — nieskończona lista (feed)

```tsx
// hooks/useFeed.ts
import { useInfiniteQuery } from "@tanstack/react-query";

export function useFeed() {
  return useInfiniteQuery({
    queryKey: ["feed"],
    queryFn: ({ pageParam }) => pobierzFeed(pageParam),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (ostatnia) =>
      ostatnia.pagination?.hasMore ? ostatnia.pagination.cursor : undefined,
  });
}

// Użycie
function FeedEkran() {
  const { data, fetchNextPage, hasNextPage, isFetchingNextPage, isLoading } = useFeed();
  const posty = data?.pages.flatMap(s => s.data) ?? []; // spłaszcz strony

  return (
    <FlatList
      data={posty}
      keyExtractor={p => p.id}
      renderItem={({ item }) => <KartaPostu post={item} />}
      onEndReached={() => hasNextPage && fetchNextPage()}
      onEndReachedThreshold={0.5}
      ListFooterComponent={isFetchingNextPage ? <ActivityIndicator /> : null}
    />
  );
}
```

---

> ### ZADANIE 11
> Przebuduj BrowserRas z Etapu 1 używając TanStack Query.
> - `useQuery` dla listy ras (z parametrem wyszukiwania)
> - `useQuery` dla szczegółów rasy
> - Pull-to-refresh przez `refetch`
>
> Przetestuj cache: wejdź w szczegóły rasy, wróć, wejdź ponownie.
> Drugi raz nie powinno być spinnera — dane są w cache.

---

## Lekcja 12 — Zustand: Globalny stan aplikacji

TanStack Query zarządza "stanem serwera" — danymi z API.
Zustand zarządza "stanem klienta" — danymi które nie przychodzą z API:
token JWT, dane zalogowanego użytkownika, preferencje UI.

```bash
npm install zustand @react-native-async-storage/async-storage
```

```typescript
// stores/authStore.ts
import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";
import AsyncStorage from "@react-native-async-storage/async-storage";

interface Uzytkownik {
  id: string; username: string; email: string; avatarUrl: string | null;
}

interface AuthStore {
  uzytkownik: Uzytkownik | null;
  accessToken: string | null;
  isZalogowany: boolean;

  zaloguj: (user: Uzytkownik, token: string) => void;
  wyloguj: () => void;
  zaktualizuj: (dane: Partial<Uzytkownik>) => void;
}

export const useAuthStore = create<AuthStore>()(
  persist(
    (set) => ({
      uzytkownik: null,
      accessToken: null,
      isZalogowany: false,

      zaloguj: (uzytkownik, accessToken) =>
        set({ uzytkownik, accessToken, isZalogowany: true }),

      wyloguj: () =>
        set({ uzytkownik: null, accessToken: null, isZalogowany: false }),

      zaktualizuj: (dane) =>
        set(s => ({ uzytkownik: s.uzytkownik ? { ...s.uzytkownik, ...dane } : null })),
    }),
    {
      name: "auth-storage",
      storage: createJSONStorage(() => AsyncStorage), // przeżyje zamknięcie apki
    }
  )
);
```

```tsx
// Użycie — w DOWOLNYM komponencie, bez prop drilling
import { useAuthStore } from "../../stores/authStore";

function Naglowek() {
  // Subskrybuj tylko to czego potrzebujesz — reszta nie powoduje re-renderu
  const uzytkownik = useAuthStore(s => s.uzytkownik);
  const wyloguj    = useAuthStore(s => s.wyloguj);

  return (
    <View>
      <Text>Cześć, {uzytkownik?.username}!</Text>
      <TouchableOpacity onPress={wyloguj}><Text>Wyloguj</Text></TouchableOpacity>
    </View>
  );
}
```

---

## Lekcja 13 — NativeWind: Stylowanie przez klasy

```bash
npm install nativewind tailwindcss && npx tailwindcss init
```

```tsx
// BEZ NativeWind
<TouchableOpacity style={{ backgroundColor: "#3B82F6", padding: 16, borderRadius: 8 }}>
  <Text style={{ color: "#fff", fontWeight: "bold", textAlign: "center" }}>Zaloguj</Text>
</TouchableOpacity>

// Z NativeWind — krótko, czytelnie
<TouchableOpacity className="bg-blue-500 p-4 rounded-lg">
  <Text className="text-white font-bold text-center">Zaloguj</Text>
</TouchableOpacity>
```

**Najważniejsze klasy:**

```
LAYOUT:   flex-1  flex-row  items-center  justify-center  justify-between  gap-4
SPACING:  p-4  py-2  px-6  m-4  mt-2  mb-8
KOLORY:   bg-white  bg-blue-500  bg-red-500  text-white  text-gray-600
TEKST:    text-lg  text-sm  font-bold  font-semibold
WYMIARY:  w-full  h-12  w-16 h-16
BORDER:   rounded  rounded-lg  rounded-full  border  border-gray-200
```

---

## Projekt 2 — BrowserRas: Podłączenie do backendu PetsApp

### Upewnij się że backend działa

```bash
docker compose up -d
cd backend && SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
# Swagger: http://localhost:8080/swagger-ui.html
```

### Stwórz projekt i zainstaluj wszystko

```bash
npx create-expo-app BrowserRas --template expo-template-blank-typescript
cd BrowserRas
npm install @tanstack/react-query nativewind tailwindcss expo-router
```

### Docelowa struktura

```
BrowserRas/
├── app/
│   ├── _layout.tsx          ← QueryClientProvider + Stack
│   ├── (tabs)/
│   │   ├── _layout.tsx      ← Tab: Rasy | Odkryte
│   │   ├── rasy.tsx
│   │   └── odkryte.tsx
│   └── rasa/[id].tsx
├── components/
│   ├── ui/SzukajInput.tsx
│   └── rasy/
│       ├── KartaRasy.tsx
│       └── GwiazdkiRzadkosci.tsx
├── services/breedsService.ts
├── hooks/useRasy.ts
├── types/api.ts
└── constants/config.ts      ← API_BASE_URL = "http://localhost:8080/api/v1"
```

### Samodzielna implementacja — sprawdzenie

```
[ ] Lista ras ładuje się ze spinnera i wyświetla rasy z backendu
[ ] Wyszukiwarka filtruje na żywo
[ ] Kliknięcie → ekran szczegółów z nazwą rasy i rzadkością
[ ] Powrót i ponowne wejście w tę samą rasę — BRAK spinnera (cache TanStack)
[ ] Pull-to-refresh działa
[ ] Wyłącz Wi-Fi — pojawia się komunikat błędu, nie biały ekran
[ ] npx tsc --noEmit → 0 błędów TypeScript
[ ] App.tsx ma max 15 linii
```

---

# ETAP 3 — Wdrożenie PetsApp Frontend

---

## Kontekst — backend jest gotowy

Backend PetsApp (kroki 1-8 z `implementation_plan.md`) jest ukończony.
Masz dostęp do:
- **Swagger UI:** http://localhost:8080/swagger-ui.html — pełna dokumentacja API
- **MailHog:** http://localhost:8025 — emaile rejestracji, resetowania hasła
- **MinIO:** http://localhost:9001 — zdjęcia uploadowane przez API

### Inicjalizacja projektu

```bash
cd /home/adam/coding/Pet_app_prototype

npx create-expo-app frontend --template expo-template-blank-typescript
cd frontend

npm install \
  @tanstack/react-query zustand \
  expo-router nativewind tailwindcss \
  @expo/vector-icons \
  expo-image-picker expo-camera \
  expo-notifications expo-auth-session expo-web-browser \
  @react-native-async-storage/async-storage \
  react-native-safe-area-context react-native-screens \
  react-native-reanimated
```

### Struktura projektu

```
frontend/
├── app/
│   ├── _layout.tsx               ← Root: QueryClient, SafeArea, redirect logika
│   ├── index.tsx                 ← zalogowany → /(tabs)/feed | niezalogowany → /(auth)/login
│   ├── (auth)/
│   │   ├── login.tsx
│   │   ├── register.tsx
│   │   ├── verify-email.tsx
│   │   └── forgot-password.tsx
│   ├── (tabs)/
│   │   ├── _layout.tsx           ← Feed | Aparat | Pokedex | Profil
│   │   ├── feed.tsx
│   │   ├── camera.tsx
│   │   ├── pokedex.tsx
│   │   └── profile.tsx
│   ├── catch/[id].tsx
│   ├── breed/[id].tsx
│   └── user/[id].tsx
│
├── components/
│   ├── ui/
│   │   ├── Button.tsx
│   │   ├── Input.tsx
│   │   ├── Avatar.tsx
│   │   └── LoadingSpinner.tsx
│   ├── catch/
│   │   ├── CatchCard.tsx
│   │   └── LikeButton.tsx
│   ├── feed/FeedList.tsx
│   ├── breed/BreedCard.tsx
│   └── profile/StatsRow.tsx
│
├── hooks/
│   ├── useAuth.ts
│   ├── useFeed.ts
│   ├── useBreeds.ts
│   └── useCatch.ts
│
├── services/
│   ├── apiClient.ts              ← JEDEN klient z auto-dołączaniem tokenu
│   ├── authService.ts
│   ├── feedService.ts
│   ├── breedsService.ts
│   ├── catchService.ts
│   └── uploadService.ts
│
├── stores/
│   └── authStore.ts
│
├── types/
│   └── api.ts                    ← Wszystkie typy odzwierciedlające backend DTO
│
└── constants/
    ├── config.ts                 ← API_BASE_URL (nigdy nie hardkoduj!)
    └── queryKeys.ts              ← centralne klucze TanStack Query
```

### Typy API — zacznij od nich

Zaloguj się do Swagger UI, przejrzyj endpointy i zdefiniuj typy w `types/api.ts`
**przed** napisaniem pierwszego ekranu. Muszą odpowiadać strukturze JSON z backendu.

```typescript
// types/api.ts

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  pagination?: { cursor: string; hasMore: boolean };
}

export interface User {
  id: string;
  username: string;
  email: string;
  bio: string | null;
  avatarUrl: string | null;
  isEmailVerified: boolean;
  totalCatches: number;
  uniqueBreeds: number;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  user: User;
}

export interface Breed {
  id: number;
  name: string;
  group: string;
  size: "SMALL" | "MEDIUM" | "LARGE" | "GIANT";
  rarityScore: number;
  silhouetteUrl: string | null;
  globalCatchCount: number;
}

export interface DogCatch {
  id: string;
  userId: string;
  username: string;
  userAvatarUrl: string | null;
  breedId: number;
  breedName: string;
  caption: string | null;
  photoUrl: string;
  thumbnailUrl: string;
  likeCount: number;
  commentCount: number;
  likedByCurrentUser: boolean;
  isPublic: boolean;
  caughtAt: string;
}

export interface Notification {
  id: string;
  type: "LIKE" | "COMMENT" | "FRIEND_REQUEST" | "FRIEND_ACCEPT" | "ACHIEVEMENT";
  title: string;
  body: string;
  isRead: boolean;
  createdAt: string;
}
```

### apiClient — jeden klient dla wszystkich requestów

```typescript
// services/apiClient.ts
import { useAuthStore } from "../stores/authStore";
import { API_BASE_URL } from "../constants/config";

class ApiClient {
  async get<T>(path: string, auth = true): Promise<T> {
    return this.request("GET", path, undefined, auth);
  }
  async post<T>(path: string, body?: unknown, auth = true): Promise<T> {
    return this.request("POST", path, body, auth);
  }
  async patch<T>(path: string, body: unknown, auth = true): Promise<T> {
    return this.request("PATCH", path, body, auth);
  }
  async delete<T>(path: string, auth = true): Promise<T> {
    return this.request("DELETE", path, undefined, auth);
  }

  async postFormData<T>(path: string, formData: FormData, auth = true): Promise<T> {
    const headers: HeadersInit = {};
    if (auth) {
      const token = useAuthStore.getState().accessToken;
      if (token) headers["Authorization"] = `Bearer ${token}`;
    }
    const res = await fetch(`${API_BASE_URL}${path}`, {
      method: "POST", headers, body: formData,
    });
    return this.handleResponse<T>(res);
  }

  private async request<T>(
    method: string, path: string, body?: unknown, auth = true
  ): Promise<T> {
    const headers: HeadersInit = { "Content-Type": "application/json" };
    if (auth) {
      const token = useAuthStore.getState().accessToken;
      if (token) headers["Authorization"] = `Bearer ${token}`;
    }
    const res = await fetch(`${API_BASE_URL}${path}`, {
      method, headers, body: body ? JSON.stringify(body) : undefined,
    });
    return this.handleResponse<T>(res);
  }

  private async handleResponse<T>(res: Response): Promise<T> {
    if (res.status === 401) {
      useAuthStore.getState().wyloguj();
      throw new Error("Sesja wygasła. Zaloguj się ponownie.");
    }
    const data = await res.json();
    if (!res.ok) throw new Error(data.error?.message ?? `Błąd: ${res.status}`);
    return data.data as T;
  }
}

export const apiClient = new ApiClient();
```

## Krok 9 — Ekrany autoryzacji

### Serwis

```typescript
// services/authService.ts
import { apiClient } from "./apiClient";
import { AuthResponse, User } from "../types/api";

export const authService = {
  login:               (email: string, password: string) =>
    apiClient.post<AuthResponse>("/auth/login", { email, password }, false),

  register:            (email: string, username: string, password: string) =>
    apiClient.post<{ message: string }>("/auth/register", { email, username, password }, false),

  verifyEmail:         (email: string, code: string) =>
    apiClient.post<AuthResponse>("/auth/verify-email", { email, code }, false),

  resendVerification:  (email: string) =>
    apiClient.post<{ message: string }>("/auth/resend-verification", { email }, false),

  forgotPassword:      (email: string) =>
    apiClient.post<{ message: string }>("/auth/forgot-password", { email }, false),

  logout:              (refreshToken: string) =>
    apiClient.post<void>("/auth/logout", { refreshToken }),

  getMe:               () =>
    apiClient.get<User>("/users/me"),
};
```

### Weryfikacja Kroku 9

```
[ ] Rejestracja wysyła email (MailHog: http://localhost:8025 — powinien pojawić się email)
[ ] Kod z emaila weryfikuje konto i → przekierowuje do feedu
[ ] Login zwraca token → zapisany w Zustand → persisted (przeżywa restart apki)
[ ] Wylogowanie czyści Zustand + przekierowuje do /login
[ ] Błędne hasło → czytelny komunikat, nie crash
[ ] Walidacja: email musi być emailem, hasło min 8 znaków, wielka litera, cyfra
[ ] Formularz "Nie pamiętam hasła" wysyła email z linkiem
```

## Krok 10 — Feed + Upload zdjęcia

### Upload multipart

```typescript
// services/uploadService.ts
import { apiClient } from "./apiClient";
import { DogCatch } from "../types/api";

export async function utworzZlapanie(params: {
  zdjecieUri: string;
  zdjecieMime: string;
  zdjecie​Nazwa: string;
  breedId: number;
  caption?: string;
  isPublic: boolean;
}): Promise<DogCatch> {
  const form = new FormData();
  form.append("photo", {
    uri: params.zdjecieUri,
    type: params.zdjecieMime,
    name: params.zdjecie​Nazwa,
  } as unknown as Blob);
  form.append("breedId", String(params.breedId));
  if (params.caption) form.append("caption", params.caption);
  form.append("isPublic", String(params.isPublic));
  return apiClient.postFormData<DogCatch>("/catches", form);
}
```

### Weryfikacja Kroku 10

```
[ ] Feed pokazuje posty z backendu (nie statyczne dane)
[ ] Infinite scroll — posty ładują się przy przewijaniu do końca
[ ] Lajk zmienia ikonę NATYCHMIAST (optimistic), cofa się przy błędzie sieci
[ ] Zdjęcie z galerii lub aparatu uploaduje się na serwer
[ ] MinIO (http://localhost:9001) — zdjęcie jest widoczne po uploadzie
[ ] Nowy post pojawia się w feedzie po czasie odświeżenia
```

## Krok 11 — Pokedex ras

### Weryfikacja

```
[ ] Siatka pokazuje wszystkie rasy z backendu
[ ] Odkryte — kolorowe zdjęcie | nieodkryte — szara sylwetka (silhouetteUrl)
[ ] Licznik "Odkryte: X/Y" aktualizuje się po złapaniu nowego psa
[ ] Filtrowanie po grupie FCI
[ ] Kliknięcie rasy → szczegóły + globalne statystyki (ile osób złapało)
```

## Krok 12 — Profil, Znajomi, Achievementy, Push

### Weryfikacja

```
[ ] Edycja profilu (username, bio) działa i zapisuje się do bazy
[ ] Upload avatara przez expo-image-picker + POST /users/me/avatar
[ ] Wyszukiwanie użytkowników po username
[ ] Zaproszenie → wysłanie, akceptacja, odrzucenie
[ ] Badge (czerwona liczba) na zakładce powiadomień
[ ] Push notification przy nowym lajku (token rejestrowany po zalogowaniu)
[ ] Ekran achievementów: odblokowane kolorowe, zablokowane szare ze znacznikiem postępu
```

---

## Reguły obowiązkowe — lista kontrolna przed commitem

```
ARCHITEKTURA:
  [ ] fetch TYLKO w services/ — zero fetch w hookach, komponentach, storach
  [ ] Stan serwera w TanStack Query — nie w Zustand
  [ ] Logika > 15 linii wyciągnięta do hooka
  [ ] Jeden plik = jeden komponent = jedna odpowiedzialność

TYPESCRIPT:
  [ ] Zero "any" — sprawdź: npx tsc --noEmit → 0 błędów
  [ ] Typy API w types/api.ts — nie duplikuj interfejsów

LISTY I STANY:
  [ ] FlatList zamiast map() w ScrollView dla list danych
  [ ] keyExtractor używa .id — nie indeksu tablicy
  [ ] Obsłużone: isLoading + isError + data — wszystkie trzy
  [ ] Pusta lista = czytelny komunikat UI

OPTYMALIZACJE:
  [ ] React.memo dla komponentów w FlatList renderItem
  [ ] useCallback dla funkcji przekazywanych jako props w dół
```

---

## Słowniczek

| Termin | Co to znaczy |
|---|---|
| **Komponent** | Funkcja zwracająca JSX — podstawowy klocek UI |
| **Props** | Dane wejściowe komponentu (jak argumenty funkcji) |
| **Stan (state)** | Wewnętrzne dane które mogą się zmieniać i powodują re-render |
| **Re-render** | Ponowne wywołanie funkcji komponentu gdy zmieni się stan lub props |
| **Hook** | Funkcja zaczynająca się od `use` — zarządza stanem lub efektami |
| **JSX** | Składnia XML w plikach `.tsx` — kompiluje się do JavaScript |
| **Promise** | Obiekt = "obietnica" wartości w przyszłości (wynik async operacji) |
| **async/await** | Składnia do pracy z Promises bez `.then().catch()` |
| **REST API** | Konwencja HTTP: GET=pobierz, POST=utwórz, PATCH=zmień, DELETE=usuń |
| **JWT** | Token autoryzacji w nagłówku `Authorization: Bearer <token>` |
| **Cache** | TanStack Query zapamiętuje odpowiedzi — nie wychodzi do sieci za te same dane |
| **Mutacja** | Operacja zmieniająca dane na serwerze (POST, PATCH, DELETE) |
| **Optimistic update** | Zmieniasz UI przed odpowiedzią serwera — apka wydaje się szybsza |
| **Cursor pagination** | Paginacja przez "wskaźnik" zamiast numeru strony |
| **Destrukturyzacja** | `const { imie } = user` — wyciąganie właściwości z obiektu |
| **Spread** | `{ ...stary, imie: "Nowe" }` — kopia obiektu z modyfikacją |
| **Optional chaining** | `user?.bio` — bezpieczny dostęp, nie crashuje gdy null |
| **Nullish coalescing** | `bio ?? "Brak"` — wartość domyślna gdy null lub undefined |
| **Generic** | `ApiResponse<T>` — T to placeholder na konkretny typ |
| **Singleton** | Jedna instancja klasy w całej aplikacji (np. `apiClient`) |
| **Prop drilling** | Przekazywanie props przez wiele poziomów — Zustand to rozwiązuje |
| **Separacja odpowiedzialności** | Każdy plik robi jedną rzecz — klucz do utrzymywalnego kodu |

---

## Gdzie szukać pomocy

| Co szukasz | Gdzie |
|---|---|
| React Native — komponenty | https://reactnative.dev/docs/components-and-apis |
| Expo — moduły (kamera, powiadomienia) | https://docs.expo.dev |
| Expo Router — nawigacja | https://docs.expo.dev/router/introduction |
| TanStack Query — guides | https://tanstack.com/query/latest/docs/framework/react/guides/queries |
| Zustand | https://docs.pmnd.rs/zustand/getting-started/introduction |
| NativeWind — klasy | https://www.nativewind.dev/tailwind/core-concepts/sizing |
| TypeScript — codzienne typy | https://www.typescriptlang.org/docs/handbook/2/everyday-types.html |
| **API PetsApp** | http://localhost:8080/swagger-ui.html |
| **Emaile DEV** | http://localhost:8025 (MailHog) |
| **Zdjęcia DEV** | http://localhost:9001 (MinIO, login: petsapp_minio) |

---
