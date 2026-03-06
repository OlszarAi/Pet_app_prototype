# Zadanie Mobile 02 — Rejestracja, Weryfikacja Email, Logowanie, Wylogowanie

**Cel:** Napisać kompletny flow autentykacji: rejestracja → weryfikacja kodu z maila → logowanie → ekran zalogowanego użytkownika → wylogowanie.

**Czas:** ~2–3h  
**Trudność:** ⭐⭐⭐

---

## Co będziesz robić

```
mobile/
  src/
    api/
      auth.ts                  ← (1) funkcje API — register, login, verifyEmail, logout
    stores/
      authStore.ts             ← (2) Zustand store — tokeny, stan auth, persist
  app/
    (auth)/
      _layout.tsx              ← (3) Stack navigator dla ekranów auth
      login.tsx                ← (4) ekran logowania
      register.tsx             ← (5) ekran rejestracji
      verify-email.tsx         ← (6) ekran — wpisz 6-cyfrowy kod z maila
    (tabs)/
      _layout.tsx              ← (7) Tab navigator (tylko dla zalogowanych)
      index.tsx                ← (8) ekran główny — "Zalogowano", przycisk wyloguj
    _layout.tsx                ← (9) root layout — guard autentykacji
    index.tsx                  ← (10) index — przekierowanie wg stanu auth
```

---

## Kontekst architektury

```
AuthStore (Zustand + AsyncStorage)
  ├─ accessToken / refreshToken    ← przechowywane lokalnie
  ├─ login(tokens)                 ← zapisuje tokeny
  └─ logout()                      ← czyści tokeny

Ekrany (auth)/                     Ekrany (tabs)/
  ├─ login.tsx                       └─ index.tsx
  ├─ register.tsx                         └─ "Zalogowano jako..."
  └─ verify-email.tsx                     └─ przycisk "Wyloguj"
         ↕                                       ↕
    src/api/auth.ts                   authStore.logout()
         ↕
    axios → backend POST /auth/...
```

**Flow rejestracji:**
1. Użytkownik wypełnia formularz rejestracji → `POST /auth/register`
2. Backend wysyła 6-cyfrowy kod na maila
3. Użytkownik wpisuje kod → `POST /auth/verify-email`
4. Backend zwraca tokeny → zalogowany!

**Flow logowania:**
1. Użytkownik wpisuje email + hasło → `POST /auth/login`
2. Backend zwraca tokeny → zalogowany!

---

## Krok 0 — Instalacja paczek

Potrzebujesz dwóch nowych bibliotek:

```bash
cd mobile
npx expo install @react-native-async-storage/async-storage
npx expo install zustand
```

| Paczka | Do czego |
|---|---|
| `@react-native-async-storage/async-storage` | Lokalne przechowywanie tokenów (odpowiednik localStorage w web) |
| `zustand` | Lekki state manager — globalny store dla stanu auth |

---

## Krok 1 — Funkcje API (`src/api/auth.ts`)

Stwórz plik `src/api/auth.ts`.

Funkcje muszą:
- używać `apiClient` z `src/api/client.ts`
- zwrócić dane lub rzucić błąd (axios sam rzuca przy 4xx/5xx)

```ts
// src/api/auth.ts
import apiClient from './client';

// Typy odpowiedzi backendu
// Backend zawsze zwraca: { success: true, data: { ... } }

type ApiResponse<T> = {
  data: T;
};

type TokensData = {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
};

type MessageData = {
  message: string;
};

// ─────────────────────────────────────────────
// Rejestracja
// POST /auth/register
// Zwraca wiadomość (nie tokeny — trzeba zweryfikować email)
// ─────────────────────────────────────────────
export async function registerUser(
  email: string,
  username: string,
  password: string
): Promise<string> {
  const response = await apiClient.post<ApiResponse<MessageData>>('/auth/register', {
    email,
    username,
    password,
  });
  return response.data.data.message;
}

// ─────────────────────────────────────────────
// Weryfikacja emaila — wpisanie 6-cyfrowego kodu
// POST /auth/verify-email
// Zwraca tokeny (od razu zalogowany)
// ─────────────────────────────────────────────
export async function verifyEmail(
  email: string,
  code: string
): Promise<TokensData> {
  const response = await apiClient.post<ApiResponse<TokensData>>('/auth/verify-email', {
    email,
    code,
  });
  return response.data.data;
}

// ─────────────────────────────────────────────
// Ponowne wysłanie kodu weryfikacyjnego
// POST /auth/resend-verification
// ─────────────────────────────────────────────
export async function resendVerificationCode(email: string): Promise<string> {
  const response = await apiClient.post<ApiResponse<MessageData>>(
    '/auth/resend-verification',
    { email }
  );
  return response.data.data.message;
}

// ─────────────────────────────────────────────
// Logowanie
// POST /auth/login
// ─────────────────────────────────────────────
export async function loginUser(
  email: string,
  password: string
): Promise<TokensData> {
  const response = await apiClient.post<ApiResponse<TokensData>>('/auth/login', {
    email,
    password,
  });
  return response.data.data;
}

// ─────────────────────────────────────────────
// Wylogowanie
// POST /auth/logout
// Wymaga refreshToken — backend unieważnia token dla tego urządzenia
// ─────────────────────────────────────────────
export async function logoutUser(refreshToken: string): Promise<void> {
  await apiClient.post('/auth/logout', { refreshToken });
}
```

> **Uwaga:** `loginUser` i `verifyEmail` zwracają `TokensData` — obiekt z `accessToken`, `refreshToken`, `expiresIn`.  
> Te tokeny musisz zapisać do store (krok 2).

---

## Krok 2 — Auth Store (`src/stores/authStore.ts`)

To jest "serce" całego flow auth. Store przechowuje tokeny i wystawia akcje `login`/`logout`.

```ts
// src/stores/authStore.ts
import { create } from 'zustand';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { logoutUser } from '../api/auth';

// Klucze w AsyncStorage
const ACCESS_TOKEN_KEY = 'auth_access_token';
const REFRESH_TOKEN_KEY = 'auth_refresh_token';

type AuthState = {
  accessToken: string | null;
  refreshToken: string | null;
  isReady: boolean; // true gdy store załadował dane z AsyncStorage
  login: (tokens: { accessToken: string; refreshToken: string }) => Promise<void>;
  logout: () => Promise<void>;
  loadFromStorage: () => Promise<void>;
};

export const useAuthStore = create<AuthState>((set, get) => ({
  accessToken: null,
  refreshToken: null,
  isReady: false,

  // Zapisuje tokeny w pamięci i na dysku
  login: async ({ accessToken, refreshToken }) => {
    await AsyncStorage.multiSet([
      [ACCESS_TOKEN_KEY, accessToken],
      [REFRESH_TOKEN_KEY, refreshToken],
    ]);
    set({ accessToken, refreshToken });
  },

  // Wylogowuje — informuje backend, czyści pamięć
  logout: async () => {
    const { refreshToken } = get();
    try {
      if (refreshToken) {
        await logoutUser(refreshToken);
      }
    } catch {
      // Nawet jeśli backend nie odpowie, czyścimy lokalnie
    } finally {
      await AsyncStorage.multiRemove([ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY]);
      set({ accessToken: null, refreshToken: null });
    }
  },

  // Wczytuje tokeny przy starcie aplikacji
  loadFromStorage: async () => {
    const values = await AsyncStorage.multiGet([ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY]);
    const accessToken = values[0][1];
    const refreshToken = values[1][1];
    set({ accessToken, refreshToken, isReady: true });
  },
}));
```

<details>
<summary>💡 Co to jest Zustand?</summary>

Zustand to minimalny state manager. `create()` tworzy hook — możesz go wywołać w dowolnym komponencie:
```ts
const { accessToken, login, logout } = useAuthStore();
```
Każda zmiana przez `set()` powoduje re-render komponentów, które subskrybują ten kawałek store.

</details>

<details>
<summary>💡 Dlaczego isReady?</summary>

`AsyncStorage.multiGet` jest asynchroniczne. Na starcie aplikacji musimy poczekać aż tokeny się załadują zanim zdecydujemy, czy pokazać ekrany auth czy główne.  
`isReady: false` = "jeszcze ładuję" → pokaż splash / spinner.  
`isReady: true` = "sprawdziłem" → zdecyduj dokąd nawigować.

</details>

---

## Krok 3 — Root layout z guardiem (`app/_layout.tsx`)

Root layout musi:
1. Załadować tokeny z AsyncStorage przy starcie
2. Otoczyć aplikację w `QueryClientProvider`
3. Renderować `<Slot />` (aktualna strona Expo Router)

```tsx
// app/_layout.tsx
import { useEffect } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Slot } from 'expo-router';
import { useAuthStore } from '../src/stores/authStore';

const queryClient = new QueryClient();

export default function RootLayout() {
  const loadFromStorage = useAuthStore((s) => s.loadFromStorage);

  useEffect(() => {
    loadFromStorage();
  }, []);

  return (
    <QueryClientProvider client={queryClient}>
      <Slot />
    </QueryClientProvider>
  );
}
```

> **Dlaczego nie ma tu przekierowania?** Guard auth robimy w `app/index.tsx` — o tym w kroku 9.

---

## Krok 4 — Route główna z przekierowaniem (`app/index.tsx`)

Ten plik decyduje, dokąd przekierować użytkownika przy starcie.

```tsx
// app/index.tsx
import { Redirect } from 'expo-router';
import { View, ActivityIndicator, StyleSheet } from 'react-native';
import { useAuthStore } from '../src/stores/authStore';

export default function Index() {
  const { isReady, accessToken } = useAuthStore();

  // Czekaj aż AsyncStorage się załaduje
  if (!isReady) {
    return (
      <View style={styles.container}>
        <ActivityIndicator size="large" color="#6C63FF" />
      </View>
    );
  }

  // Jeśli zalogowany → ekran główny, jeśli nie → logowanie
  if (accessToken) {
    return <Redirect href="/(tabs)/" />;
  }

  return <Redirect href="/(auth)/login" />;
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0a0a0a',
    alignItems: 'center',
    justifyContent: 'center',
  },
});
```

---

## Krok 5 — Layout grupy auth (`app/(auth)/_layout.tsx`)

Grupa `(auth)` używa Stack navigatora — ekrany układają się w stos (login → register → verify-email).

```tsx
// app/(auth)/_layout.tsx
import { Stack } from 'expo-router';

export default function AuthLayout() {
  return (
    <Stack
      screenOptions={{
        headerShown: false,        // bez nagłówka — robimy własny design
        contentStyle: { backgroundColor: '#0a0a0a' },
      }}
    />
  );
}
```

---

## Krok 6 — Ekran logowania (`app/(auth)/login.tsx`)

```tsx
// app/(auth)/login.tsx
import { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  Alert,
} from 'react-native';
import { router } from 'expo-router';
import { loginUser } from '../../src/api/auth';
import { useAuthStore } from '../../src/stores/authStore';

export default function LoginScreen() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const login = useAuthStore((s) => s.login);

  async function handleLogin() {
    if (!email || !password) {
      Alert.alert('Błąd', 'Wypełnij wszystkie pola');
      return;
    }

    setIsLoading(true);
    try {
      const tokens = await loginUser(email, password);
      await login(tokens);
      router.replace('/(tabs)/');
    } catch (err: any) {
      const message =
        err?.response?.data?.error?.message ?? 'Nie udało się zalogować';
      Alert.alert('Błąd logowania', message);
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <View style={styles.inner}>
        <Text style={styles.title}>Witaj z powrotem 🐾</Text>
        <Text style={styles.subtitle}>Zaloguj się do konta</Text>

        <View style={styles.form}>
          <Text style={styles.label}>Email</Text>
          <TextInput
            style={styles.input}
            placeholder="adres@email.com"
            placeholderTextColor="#555"
            value={email}
            onChangeText={setEmail}
            autoCapitalize="none"
            keyboardType="email-address"
            autoComplete="email"
          />

          <Text style={styles.label}>Hasło</Text>
          <TextInput
            style={styles.input}
            placeholder="••••••••"
            placeholderTextColor="#555"
            value={password}
            onChangeText={setPassword}
            secureTextEntry
            autoComplete="password"
          />

          <TouchableOpacity
            style={[styles.button, isLoading && styles.buttonDisabled]}
            onPress={handleLogin}
            disabled={isLoading}
          >
            <Text style={styles.buttonText}>
              {isLoading ? 'Logowanie...' : 'Zaloguj się'}
            </Text>
          </TouchableOpacity>
        </View>

        <TouchableOpacity onPress={() => router.push('/(auth)/register')}>
          <Text style={styles.link}>Nie masz konta? <Text style={styles.linkAccent}>Zarejestruj się</Text></Text>
        </TouchableOpacity>
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0a0a0a',
  },
  inner: {
    flex: 1,
    paddingHorizontal: 28,
    justifyContent: 'center',
  },
  title: {
    color: '#ffffff',
    fontSize: 32,
    fontWeight: '700',
    marginBottom: 8,
  },
  subtitle: {
    color: '#888',
    fontSize: 16,
    marginBottom: 40,
  },
  form: {
    marginBottom: 32,
    gap: 8,
  },
  label: {
    color: '#aaa',
    fontSize: 13,
    fontWeight: '600',
    marginTop: 16,
    marginBottom: 4,
    textTransform: 'uppercase',
    letterSpacing: 0.8,
  },
  input: {
    backgroundColor: '#1a1a1a',
    borderWidth: 1,
    borderColor: '#2a2a2a',
    borderRadius: 12,
    paddingHorizontal: 16,
    paddingVertical: 14,
    color: '#fff',
    fontSize: 16,
  },
  button: {
    backgroundColor: '#6C63FF',
    borderRadius: 12,
    paddingVertical: 16,
    alignItems: 'center',
    marginTop: 24,
  },
  buttonDisabled: {
    opacity: 0.6,
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '700',
  },
  link: {
    color: '#666',
    fontSize: 15,
    textAlign: 'center',
  },
  linkAccent: {
    color: '#6C63FF',
    fontWeight: '600',
  },
});
```

<details>
<summary>💡 Co robi KeyboardAvoidingView?</summary>

Na iOS klawiatura najeżdża na ekran i przykrywa inputy. `KeyboardAvoidingView` z `behavior="padding"` przesuwa zawartość w górę gdy klawiatura się pojawia.  
Na Androidzie ten problem jest rozwiązany inaczej — dlatego `Platform.OS === 'ios' ? 'padding' : 'height'`.

</details>

<details>
<summary>💡 Skąd biorę błąd z odpowiedzi backendu?</summary>

Backend zwraca błędy w strukturze:
```json
{ "success": false, "error": { "code": "UNAUTHORIZED", "message": "..." } }
```
Axios rzuca wyjątek przy 4xx/5xx, a ciało odpowiedzi jest w `err.response.data`.  
Dlatego: `err?.response?.data?.error?.message`.  
`??` (nullish coalescing) daje fallback gdy wartość jest `null`/`undefined`.

</details>

---

## Krok 7 — Ekran rejestracji (`app/(auth)/register.tsx`)

```tsx
// app/(auth)/register.tsx
import { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  Alert,
  ScrollView,
} from 'react-native';
import { router } from 'expo-router';
import { registerUser } from '../../src/api/auth';

export default function RegisterScreen() {
  const [email, setEmail] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  async function handleRegister() {
    if (!email || !username || !password) {
      Alert.alert('Błąd', 'Wypełnij wszystkie pola');
      return;
    }

    setIsLoading(true);
    try {
      await registerUser(email, username, password);
      // Sukces — przenosimy na ekran weryfikacji emaila
      // Przekazujemy email jako parametr trasy
      router.push({ pathname: '/(auth)/verify-email', params: { email } });
    } catch (err: any) {
      const errorData = err?.response?.data?.error;

      // Błędy walidacji pól (np. "username za krótki")
      if (errorData?.fields) {
        const fieldErrors = Object.values(errorData.fields).join('\n');
        Alert.alert('Błąd walidacji', fieldErrors);
        return;
      }

      const message = errorData?.message ?? 'Rejestracja nie powiodła się';
      Alert.alert('Błąd rejestracji', message);
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <ScrollView contentContainerStyle={styles.inner} keyboardShouldPersistTaps="handled">
        <TouchableOpacity style={styles.backButton} onPress={() => router.back()}>
          <Text style={styles.backText}>← Wróć</Text>
        </TouchableOpacity>

        <Text style={styles.title}>Utwórz konto</Text>
        <Text style={styles.subtitle}>Dołącz do społeczności 🐾</Text>

        <View style={styles.form}>
          <Text style={styles.label}>Email</Text>
          <TextInput
            style={styles.input}
            placeholder="adres@email.com"
            placeholderTextColor="#555"
            value={email}
            onChangeText={setEmail}
            autoCapitalize="none"
            keyboardType="email-address"
            autoComplete="email"
          />

          <Text style={styles.label}>Nazwa użytkownika</Text>
          <TextInput
            style={styles.input}
            placeholder="np. jan_kowalski"
            placeholderTextColor="#555"
            value={username}
            onChangeText={setUsername}
            autoCapitalize="none"
            autoComplete="username"
          />
          <Text style={styles.hint}>3–30 znaków, tylko litery, cyfry i _</Text>

          <Text style={styles.label}>Hasło</Text>
          <TextInput
            style={styles.input}
            placeholder="minimum 8 znaków"
            placeholderTextColor="#555"
            value={password}
            onChangeText={setPassword}
            secureTextEntry
            autoComplete="new-password"
          />

          <TouchableOpacity
            style={[styles.button, isLoading && styles.buttonDisabled]}
            onPress={handleRegister}
            disabled={isLoading}
          >
            <Text style={styles.buttonText}>
              {isLoading ? 'Tworzę konto...' : 'Zarejestruj się'}
            </Text>
          </TouchableOpacity>
        </View>

        <TouchableOpacity onPress={() => router.back()}>
          <Text style={styles.link}>
            Masz już konto?{' '}
            <Text style={styles.linkAccent}>Zaloguj się</Text>
          </Text>
        </TouchableOpacity>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0a0a0a',
  },
  inner: {
    paddingHorizontal: 28,
    paddingTop: 60,
    paddingBottom: 40,
  },
  backButton: {
    marginBottom: 32,
  },
  backText: {
    color: '#6C63FF',
    fontSize: 16,
  },
  title: {
    color: '#ffffff',
    fontSize: 32,
    fontWeight: '700',
    marginBottom: 8,
  },
  subtitle: {
    color: '#888',
    fontSize: 16,
    marginBottom: 40,
  },
  form: {
    marginBottom: 32,
    gap: 4,
  },
  label: {
    color: '#aaa',
    fontSize: 13,
    fontWeight: '600',
    marginTop: 16,
    marginBottom: 4,
    textTransform: 'uppercase',
    letterSpacing: 0.8,
  },
  input: {
    backgroundColor: '#1a1a1a',
    borderWidth: 1,
    borderColor: '#2a2a2a',
    borderRadius: 12,
    paddingHorizontal: 16,
    paddingVertical: 14,
    color: '#fff',
    fontSize: 16,
  },
  hint: {
    color: '#555',
    fontSize: 12,
    marginTop: 4,
  },
  button: {
    backgroundColor: '#6C63FF',
    borderRadius: 12,
    paddingVertical: 16,
    alignItems: 'center',
    marginTop: 24,
  },
  buttonDisabled: {
    opacity: 0.6,
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '700',
  },
  link: {
    color: '#666',
    fontSize: 15,
    textAlign: 'center',
  },
  linkAccent: {
    color: '#6C63FF',
    fontWeight: '600',
  },
});
```

---

## Krok 8 — Ekran weryfikacji emaila (`app/(auth)/verify-email.tsx`)

Ten ekran dostaje `email` jako parametr trasy (przekazany z ekranu rejestracji).

```tsx
// app/(auth)/verify-email.tsx
import { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  Alert,
} from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { verifyEmail, resendVerificationCode } from '../../src/api/auth';
import { useAuthStore } from '../../src/stores/authStore';

export default function VerifyEmailScreen() {
  // Parametr przekazany przez router.push({ params: { email } })
  const { email } = useLocalSearchParams<{ email: string }>();
  const [code, setCode] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isResending, setIsResending] = useState(false);
  const login = useAuthStore((s) => s.login);

  async function handleVerify() {
    if (code.length !== 6) {
      Alert.alert('Błąd', 'Kod musi mieć dokładnie 6 cyfr');
      return;
    }

    setIsLoading(true);
    try {
      const tokens = await verifyEmail(email, code);
      await login(tokens);
      router.replace('/(tabs)/');
    } catch (err: any) {
      const message =
        err?.response?.data?.error?.message ?? 'Nieprawidłowy kod';
      Alert.alert('Błąd weryfikacji', message);
    } finally {
      setIsLoading(false);
    }
  }

  async function handleResend() {
    setIsResending(true);
    try {
      await resendVerificationCode(email);
      Alert.alert('Wysłano!', 'Sprawdź skrzynkę emailową');
    } catch (err: any) {
      Alert.alert('Błąd', 'Nie udało się wysłać kodu');
    } finally {
      setIsResending(false);
    }
  }

  return (
    <View style={styles.container}>
      <View style={styles.inner}>
        <Text style={styles.emoji}>📬</Text>
        <Text style={styles.title}>Sprawdź email</Text>
        <Text style={styles.subtitle}>
          Wysłaliśmy 6-cyfrowy kod na{'\n'}
          <Text style={styles.email}>{email}</Text>
        </Text>

        <TextInput
          style={styles.codeInput}
          placeholder="000000"
          placeholderTextColor="#444"
          value={code}
          onChangeText={(text) => setCode(text.replace(/[^0-9]/g, ''))}
          keyboardType="number-pad"
          maxLength={6}
          textAlign="center"
        />

        <TouchableOpacity
          style={[styles.button, isLoading && styles.buttonDisabled]}
          onPress={handleVerify}
          disabled={isLoading || code.length !== 6}
        >
          <Text style={styles.buttonText}>
            {isLoading ? 'Weryfikuję...' : 'Potwierdź kod'}
          </Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.resendButton}
          onPress={handleResend}
          disabled={isResending}
        >
          <Text style={styles.resendText}>
            {isResending ? 'Wysyłanie...' : 'Wyślij kod ponownie'}
          </Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0a0a0a',
  },
  inner: {
    flex: 1,
    paddingHorizontal: 28,
    alignItems: 'center',
    justifyContent: 'center',
  },
  emoji: {
    fontSize: 64,
    marginBottom: 24,
  },
  title: {
    color: '#fff',
    fontSize: 28,
    fontWeight: '700',
    marginBottom: 12,
    textAlign: 'center',
  },
  subtitle: {
    color: '#888',
    fontSize: 16,
    textAlign: 'center',
    lineHeight: 24,
    marginBottom: 40,
  },
  email: {
    color: '#6C63FF',
    fontWeight: '600',
  },
  codeInput: {
    backgroundColor: '#1a1a1a',
    borderWidth: 2,
    borderColor: '#6C63FF',
    borderRadius: 16,
    width: '100%',
    paddingVertical: 20,
    color: '#fff',
    fontSize: 32,
    fontWeight: '700',
    letterSpacing: 12,
    marginBottom: 24,
  },
  button: {
    backgroundColor: '#6C63FF',
    borderRadius: 12,
    paddingVertical: 16,
    alignItems: 'center',
    width: '100%',
    marginBottom: 16,
  },
  buttonDisabled: {
    opacity: 0.4,
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '700',
  },
  resendButton: {
    paddingVertical: 12,
  },
  resendText: {
    color: '#6C63FF',
    fontSize: 15,
  },
});
```

<details>
<summary>💡 Co to useLocalSearchParams?</summary>

`useLocalSearchParams()` z `expo-router` zwraca parametry przekazane do aktualnej trasy.  
Gdy nawigujesz przez `router.push({ pathname: '/(auth)/verify-email', params: { email: 'a@b.com' } })`,  
to `useLocalSearchParams()` zwróci `{ email: 'a@b.com' }`.

</details>

<details>
<summary>💡 Dlaczego .replace(/[^0-9]/g, '')?</summary>

`replace(/[^0-9]/g, '')` usuwa wszystko co nie jest cyfrą.  
Użytkownik może wkleić kod z maila który ma spacje lub myślniki — to je czyści automatycznie.  
`keyboardType="number-pad"` pokazuje tylko klawiaturę numeryczną.

</details>

---

## Krok 9 — Layout tabs (`app/(tabs)/_layout.tsx`)

Ekrany tabs wymagają zalogowania. Tutaj dodajemy guard — jeśli brak tokenu, wracamy do logowania.

```tsx
// app/(tabs)/_layout.tsx
import { Tabs } from 'expo-router';
import { useAuthStore } from '../../src/stores/authStore';
import { Redirect } from 'expo-router';

export default function TabsLayout() {
  const accessToken = useAuthStore((s) => s.accessToken);

  // Guard — niezalogowani nie mają tu wstępu
  if (!accessToken) {
    return <Redirect href="/(auth)/login" />;
  }

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarStyle: {
          backgroundColor: '#111',
          borderTopColor: '#222',
        },
        tabBarActiveTintColor: '#6C63FF',
        tabBarInactiveTintColor: '#666',
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: 'Strona główna',
          tabBarLabel: 'Dom',
        }}
      />
    </Tabs>
  );
}
```

---

## Krok 10 — Ekran główny dla zalogowanego (`app/(tabs)/index.tsx`)

```tsx
// app/(tabs)/index.tsx
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Alert,
} from 'react-native';
import { useAuthStore } from '../../src/stores/authStore';

export default function HomeScreen() {
  const { logout } = useAuthStore();

  async function handleLogout() {
    Alert.alert(
      'Wylogowanie',
      'Czy na pewno chcesz się wylogować?',
      [
        { text: 'Anuluj', style: 'cancel' },
        {
          text: 'Wyloguj',
          style: 'destructive',
          onPress: async () => {
            await logout();
            // Expo Router automatycznie wykryje brak tokenu
            // i przekieruje przez guard w (tabs)/_layout.tsx
          },
        },
      ]
    );
  }

  return (
    <View style={styles.container}>
      <View style={styles.card}>
        <Text style={styles.emoji}>✅</Text>
        <Text style={styles.title}>Zalogowano!</Text>
        <Text style={styles.subtitle}>
          Jesteś zalogowany i możesz korzystać z aplikacji.
        </Text>
      </View>

      <TouchableOpacity style={styles.logoutButton} onPress={handleLogout}>
        <Text style={styles.logoutText}>Wyloguj się</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0a0a0a',
    paddingHorizontal: 28,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 24,
  },
  card: {
    backgroundColor: '#111',
    borderRadius: 20,
    borderWidth: 1,
    borderColor: '#1e1e1e',
    paddingVertical: 40,
    paddingHorizontal: 32,
    alignItems: 'center',
    width: '100%',
  },
  emoji: {
    fontSize: 56,
    marginBottom: 20,
  },
  title: {
    color: '#fff',
    fontSize: 28,
    fontWeight: '700',
    marginBottom: 12,
  },
  subtitle: {
    color: '#888',
    fontSize: 16,
    textAlign: 'center',
    lineHeight: 24,
  },
  logoutButton: {
    borderWidth: 1,
    borderColor: '#e53935',
    borderRadius: 12,
    paddingVertical: 14,
    paddingHorizontal: 40,
  },
  logoutText: {
    color: '#e53935',
    fontSize: 16,
    fontWeight: '600',
  },
});
```

<details>
<summary>💡 Jak działa wylogowanie bez ręcznego redirectu?</summary>

Po wywołaniu `logout()` store ustawia `accessToken: null`.  
Guard w `(tabs)/_layout.tsx` subskrybuje `accessToken` — gdy zmienia się na `null`, React re-renderuje layout  
i zwraca `<Redirect href="/(auth)/login" />`. Expo Router automatycznie przenosi użytkownika.  
Nie musisz ręcznie wywoływać `router.replace()`.

</details>

---

## Jak sprawdzić czy działa

1. **Uruchom backend:**
   ```bash
   docker compose up -d
   ```

2. **Uruchom aplikację:**
   ```bash
   cd mobile
   npx expo start
   ```

3. **Przetestuj flow rejestracji:**
   - Otwórz aplikację → powinieneś zobaczyć ekran logowania
   - Kliknij "Zarejestruj się"
   - Wypełnij formularz i wyślij
   - Sprawdź email — wpisz 6-cyfrowy kod
   - Powinieneś zobaczyć ekran "Zalogowano!"

4. **Przetestuj wylogowanie:**
   - Kliknij "Wyloguj się" → potwierdź
   - Powinieneś wrócić do ekranu logowania

5. **Przetestuj persystencję:**
   - Zaloguj się → zamknij aplikację → otwórz ponownie
   - Powinieneś trafić od razu na ekran główny (tokeny zapisane)

6. **Przetestuj logowanie:**
   - Wyloguj się → zaloguj ponownie przez ekran logowania

---

## Definicja ukończenia (DoD)

- [ ] Instalacja paczek działa (`npx expo install ...`)
- [ ] Niezalogowany użytkownik widzi ekran logowania
- [ ] Rejestracja działa — backend przyjmuje dane, wysyła kod na email
- [ ] Weryfikacja emaila działa — wpisanie kodu loguje użytkownika
- [ ] Logowanie działa dla istniejącego konta
- [ ] Ekran "Zalogowano!" jest widoczny po zalogowaniu
- [ ] Wylogowanie działa i przenosi na ekran logowania
- [ ] Po zrestartowaniu aplikacji zalogowany użytkownik ląduje na ekranie głównym
- [ ] Błędy backendu są wyświetlane użytkownikowi (złe hasło, zajęty email, itp.)

---

## Słownik

| Pojęcie | Co to jest |
|---|---|
| `accessToken` | Krótkotrwały JWT (15 min) — dołączany do każdego requesta jako `Authorization: Bearer ...` |
| `refreshToken` | Długotrwały token (7 dni) — używany do odnowienia `accessToken` |
| `AsyncStorage` | Lokalna baza klucz-wartość na urządzeniu — odpowiednik `localStorage` w web |
| `Zustand` | Minimalny state manager — `create()` tworzy globalny hook store |
| `useLocalSearchParams` | Expo Router — odczytuje parametry URL przekazane do ekranu |
| `router.replace()` | Nawigacja bez możliwości cofnięcia (replace zamiast push) |
| `router.push()` | Nawigacja z możliwością cofnięcia (dodaje do stosu) |
| `<Redirect />` | Komponent Expo Router — natychmiastowe przekierowanie w `render()` |
| `KeyboardAvoidingView` | Przesuwa widok gdy pojawia się klawiatura, by nie przykrywała inputów |
| `(auth)/` | Grupa tras Expo Router — `(nazwa)` nie pojawia się w URL |
| `(tabs)/` | Grupa z Tab navigatorem — chroniona guardiem auth |

---

## Bonus — dodanie tokenu do requestów API

Teraz kiedy masz `accessToken` w store, możesz go dołączać do każdego requesta.  
Otwórz `src/api/client.ts` i dodaj interceptor:

```ts
// src/api/client.ts
import axios from 'axios';
import { useAuthStore } from '../stores/authStore';

const apiClient = axios.create({
  baseURL: process.env.EXPO_PUBLIC_API_URL,
});

// Dodaje nagłówek Authorization do każdego żądania
apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default apiClient;
```

> **Uwaga:** `useAuthStore.getState()` — poza Reactem nie możemy używać hooka `useAuthStore()`,  
> ale Zustand udostępnia `.getState()` do odczytu stanu poza komponentem.  
> Interceptory axios są wywoływane poza kontekstem React — dlatego używamy `getState()`.
