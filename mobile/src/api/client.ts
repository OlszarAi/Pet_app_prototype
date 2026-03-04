import Constants from 'expo-constants';

/**
 * Automatycznie wykrywa adres backendu:
 *  1. EXPO_PUBLIC_API_URL (zmienna w .env) — zawsze wygrywa, dobra dla prod/staging
 *  2. Constants.expoConfig.hostUri — "192.168.1.x:8081", wyciągamy samo IP
 *     i używamy portu 8080; działa na fizycznym urządzeniu iOS/Android w tej samej sieci
 *  3. Fallback na localhost (emulator iOS / web)
 */
function getBaseUrl(): string {
  if (process.env.EXPO_PUBLIC_API_URL) {
    return process.env.EXPO_PUBLIC_API_URL;
  }

  // hostUri = "192.168.1.x:8081" (adres serwera metro bundler)
  const hostUri = Constants.expoConfig?.hostUri;
  if (hostUri) {
    const host = hostUri.split(':')[0]; // samo IP bez portu
    return `http://${host}:8080/api/v1`;
  }

  return 'http://localhost:8080/api/v1';
}

const BASE_URL = getBaseUrl();

if (__DEV__) {
  console.log('[API] BASE_URL =', BASE_URL);
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: { code: string; message: string };
}

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

async function request<T>(
  path: string,
  options: RequestInit = {},
  token?: string,
): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers as Record<string, string>),
  };

  const response = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers,
  });

  const json = (await response.json()) as ApiResponse<T>;

  if (!response.ok || !json.success) {
    throw new ApiError(
      response.status,
      json.error?.code ?? 'UNKNOWN',
      json.error?.message ?? `HTTP ${response.status}`,
    );
  }

  return json.data;
}

export const api = {
  post: <T>(path: string, body: unknown, token?: string) =>
    request<T>(path, { method: 'POST', body: JSON.stringify(body) }, token),
};
