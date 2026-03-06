# Zadanie Mobile 01 — Health Screen

**Cel:** Napisać hook i ekran, który pobiera status backendu z `GET /health` i wyświetla go na ekranie.

**Czas:** ~45 min  
**Trudność:** ⭐ (intro)

---

## Co będziesz robić

Stworzysz trzy pliki:

```
mobile/
  src/
    api/
      health.ts        ← (1) funkcja fetchHealth — wywołuje API
    hooks/
      useHealth.ts     ← (2) hook useHealth — TanStack Query
  app/
    health.tsx         ← (3) ekran — wyświetla wynik
```

---

## Kontekst architektury

```
Ekran (app/health.tsx)
  └─ używa hooka (useHealth)
       └─ używa funkcji API (fetchHealth)
            └─ axios client (src/api/client.ts) → backend GET /health
```

- **`src/api/`** — czyste funkcje, które rozmawiają z backendem. Żadnego Reacta tutaj.
- **`src/hooks/`** — hooki TanStack Query, które oplatają funkcje API w cache + stany loading/error.
- **`app/`** — ekrany Expo Router. Powinny być "głupie" — tylko wyświetlają dane z hooka.

---

## Krok 1 — funkcja API (`src/api/health.ts`)

Stwórz plik `src/api/health.ts`.

Musisz napisać funkcję `fetchHealth`, która:
- używa `apiClient` z `src/api/client.ts`
- wywołuje `GET` na `/health` (uwaga: baseURL to `/api/v1`, a health jest pod `/health` — zob. podpowiedź)
- zwraca dane

```ts
// src/api/health.ts
import apiClient from './client';

// Odpowiedź backendu wygląda tak:
// { success: true, data: { status: "UP" } }
// Więc typ dla danych to:
type HealthResponse = {
  data: {
    status: string;
  };
};

export async function fetchHealth(): Promise<{ status: string }> {
  // Twój kod tutaj
  // Podpowiedź: apiClient.get<HealthResponse>(...)
  // Podpowiedź: axios zwraca { data: ... }, więc response.data to cały JSON
  //             a response.data.data to { status: "UP" }
}
```

<details>
<summary>💡 Podpowiedź — URL</summary>

`apiClient` ma `baseURL = http://localhost:8080/api/v1`  
Endpoint health to `http://localhost:8080/health` — czyli POZA `/api/v1`.

Możesz podać pełny URL bezpośrednio:
```ts
apiClient.get('http://localhost:8080/health')
// albo użyć zmiennej środowiskowej:
const BASE = process.env.EXPO_PUBLIC_API_URL?.replace('/api/v1', '') ?? '';
apiClient.get(`${BASE}/health`)
```

</details>

<details>
<summary>💡 Podpowiedź — async/await z axios</summary>

```ts
const response = await apiClient.get<HealthResponse>('/jakis/endpoint');
return response.data.data; // response.data = JSON, response.data.data = { status }
```

</details>

---

## Krok 2 — hook (`src/hooks/useHealth.ts`)

Stwórz plik `src/hooks/useHealth.ts`.

TanStack Query daje ci hook `useQuery`. Przyjmuje obiekt z:
- `queryKey` — tablica-identyfikator zapytania (np. `['health']`)
- `queryFn` — funkcja, która pobiera dane (twoja `fetchHealth`)

```ts
// src/hooks/useHealth.ts
import { useQuery } from '@tanstack/react-query';
import { fetchHealth } from '../api/health';

export function useHealth() {
  // Twój kod tutaj
  // Podpowiedź: return useQuery({ queryKey: [...], queryFn: ... })
}
```

Hook `useQuery` zwraca obiekt z polami:
- `data` — pobrane dane (lub `undefined` jeśli jeszcze nie wróciły)
- `isLoading` — `true` kiedy pierwszy raz ładuje
- `isError` — `true` kiedy coś poszło nie tak
- `error` — obiekt błędu

<details>
<summary>💡 Podpowiedź — pełny useQuery</summary>

```ts
return useQuery({
  queryKey: ['health'],
  queryFn: fetchHealth,
});
```

</details>

---

## Krok 3 — ekran (`app/health.tsx`)

Stwórz plik `app/health.tsx`.

Ekran powinien:
1. Wywołać `useHealth()`
2. Pokazać "Ładowanie..." gdy `isLoading`
3. Pokazać komunikat błędu gdy `isError`
4. Pokazać status gdy `data` jest dostępne

```tsx
// app/health.tsx
import { View, Text, StyleSheet } from 'react-native';
import { useHealth } from '../src/hooks/useHealth';

export default function HealthScreen() {
  const { data, isLoading, isError } = useHealth();

  if (isLoading) {
    return (
      <View style={styles.container}>
        <Text style={styles.text}>Łączenie z backendem...</Text>
      </View>
    );
  }

  // Twój kod na isError i na wyświetlenie data.status
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000',
    alignItems: 'center',
    justifyContent: 'center',
  },
  text: {
    color: '#fff',
    fontSize: 18,
  },
});
```

<details>
<summary>💡 Podpowiedź — isError case</summary>

```tsx
if (isError) {
  return (
    <View style={styles.container}>
      <Text style={{ color: 'red', fontSize: 18 }}>Backend niedostępny 😢</Text>
    </View>
  );
}
```

</details>

<details>
<summary>💡 Podpowiedź — wyświetlenie statusu</summary>

```tsx
return (
  <View style={styles.container}>
    <Text style={styles.text}>Status: {data?.status}</Text>
  </View>
);
```

`data?.status` — znak `?` chroni przed crashem gdy `data` jest `undefined`.

</details>

---

## Krok 4 — QueryClient Provider

Żeby TanStack Query działał, całą aplikację musisz owinąć w `QueryClientProvider`.  
Otwórz `app/_layout.tsx` (lub stwórz go jeśli nie istnieje).

```tsx
// app/_layout.tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Slot } from 'expo-router';

const queryClient = new QueryClient();

export default function RootLayout() {
  return (
    <QueryClientProvider client={queryClient}>
      <Slot />
    </QueryClientProvider>
  );
}
```

> **Dlaczego?** `QueryClientProvider` to "baza danych" cache dla całej aplikacji.  
> Każdy `useQuery` wewnątrz niej może korzystać z tego cache.  
> `<Slot />` to miejsce, gdzie Expo Router wyrenderuje aktualny ekran.

---

## Jak sprawdzić czy działa

1. Uruchom backend: `docker compose up -d` (z katalogu głównego projektu)
2. Uruchom aplikację: `cd mobile && npx expo start`
3. Otwórz ekran — wejdź na ścieżkę `/health` (lub ustaw go jako startowy tymczasowo)
4. Powinieneś zobaczyć: **Status: UP**

Żeby szybko przetestować bez nawigacji, możesz tymczasowo zamienić zawartość `app/index.tsx` na użycie `HealthScreen`.

---

## Definicja ukończenia (DoD)

- [ ] Backend działa (`docker compose up -d`)
- [ ] Ekran wyświetla `Status: UP` gdy backend jest dostępny
- [ ] Ekran wyświetla komunikat błędu gdy backend jest wyłączony (`docker compose down`)
- [ ] Stan "ładowanie" jest widoczny przez chwilę przy pierwszym uruchomieniu

---

## Słownik

| Pojęcie | Co to jest |
|---|---|
| `useQuery` | Hook TanStack Query — pobiera dane, cache'uje, zarządza stanami |
| `queryKey` | Unikalny klucz zapytania — po tym TQ wie co cache'ować |
| `queryFn` | Funkcja do pobrania danych — musi zwrócić Promise |
| `apiClient` | Instancja Axios — wie gdzie jest backend (baseURL z `.env.local`) |
| `Slot` | Expo Router — placeholder na aktualny ekran |
| `QueryClientProvider` | Context provider — udostępnia cache TQ całej aplikacji |
