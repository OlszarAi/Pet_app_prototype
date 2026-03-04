import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import * as SecureStore from 'expo-secure-store';
import { authApi, AuthTokens } from '../api/auth';

// ---- klucze magazynu ----

const KEY_ACCESS = 'auth.accessToken';
const KEY_REFRESH = 'auth.refreshToken';

// ---- typy ----

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  /** true podczas pierwszego odczytu tokenów ze secure store */
  isLoading: boolean;
}

interface AuthContextValue extends AuthState {
  saveTokens: (tokens: AuthTokens) => Promise<void>;
  logout: () => Promise<void>;
}

// ---- kontekst ----

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<AuthState>({
    accessToken: null,
    refreshToken: null,
    isAuthenticated: false,
    isLoading: true,
  });

  // Przy uruchomieniu aplikacji odczytaj zapisane tokeny
  useEffect(() => {
    (async () => {
      try {
        const [access, refresh] = await Promise.all([
          SecureStore.getItemAsync(KEY_ACCESS),
          SecureStore.getItemAsync(KEY_REFRESH),
        ]);
        if (access && refresh) {
          setState({
            accessToken: access,
            refreshToken: refresh,
            isAuthenticated: true,
            isLoading: false,
          });
        } else {
          setState((prev) => ({ ...prev, isLoading: false }));
        }
      } catch {
        setState((prev) => ({ ...prev, isLoading: false }));
      }
    })();
  }, []);

  const saveTokens = useCallback(async (tokens: AuthTokens) => {
    await Promise.all([
      SecureStore.setItemAsync(KEY_ACCESS, tokens.accessToken),
      SecureStore.setItemAsync(KEY_REFRESH, tokens.refreshToken),
    ]);
    setState({
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      isAuthenticated: true,
      isLoading: false,
    });
  }, []);

  const logout = useCallback(async () => {
    // Próba powiadomienia backendu (fire-and-forget)
    const { refreshToken, accessToken } = state;
    if (refreshToken && accessToken) {
      authApi.logout({ refreshToken }, accessToken).catch(() => {});
    }

    await Promise.all([
      SecureStore.deleteItemAsync(KEY_ACCESS),
      SecureStore.deleteItemAsync(KEY_REFRESH),
    ]);

    setState({
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
      isLoading: false,
    });
  }, [state]);

  const value = useMemo<AuthContextValue>(
    () => ({ ...state, saveTokens, logout }),
    [state, saveTokens, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
