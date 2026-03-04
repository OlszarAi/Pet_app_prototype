import React, { useState } from 'react';
import {
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { authApi } from '../api/auth';
import { ApiError } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { Button, colors, ErrorBanner, Input } from '../components/ui';
import { RootStackParamList } from '../navigation/types';

type Props = NativeStackScreenProps<RootStackParamList, 'Login'>;

export default function LoginScreen({ navigation }: Props) {
  const { saveTokens } = useAuth();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Walidacja pól przed wysłaniem
  const [fieldErrors, setFieldErrors] = useState<{
    email?: string;
    password?: string;
  }>({});

  function validate(): boolean {
    const errs: typeof fieldErrors = {};
    if (!email.trim()) errs.email = 'Email jest wymagany';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email))
      errs.email = 'Nieprawidłowy format email';
    if (!password) errs.password = 'Hasło jest wymagane';
    setFieldErrors(errs);
    return Object.keys(errs).length === 0;
  }

  async function handleLogin() {
    if (!validate()) return;
    setError('');
    setLoading(true);
    try {
      const tokens = await authApi.login({ email: email.trim(), password });
      await saveTokens(tokens);
      // Po zapisaniu tokenów nawigacja przełączy się automatycznie na ekran App
    } catch (e) {
      if (e instanceof ApiError) {
        if (e.status === 401)
          setError('Nieprawidłowy email lub hasło.');
        else if (e.status === 403)
          setError(
            'Konto nie zostało zweryfikowane. Sprawdź email i potwierdź rejestrację.',
          );
        else if (e.status === 429)
          setError('Zbyt wiele prób logowania. Poczekaj chwilę i spróbuj ponownie.');
        else
          setError(e.message);
      } else {
        setError('Nie można połączyć się z serwerem. Sprawdź internet.');
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <KeyboardAvoidingView
      style={styles.flex}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <ScrollView
        contentContainerStyle={styles.container}
        keyboardShouldPersistTaps="handled"
      >
        <View style={styles.header}>
          <Text style={styles.logo}>🐾</Text>
          <Text style={styles.title}>Witaj z powrotem!</Text>
          <Text style={styles.subtitle}>Zaloguj się do PetsApp</Text>
        </View>

        <View style={styles.form}>
          {error ? <ErrorBanner message={error} /> : null}

          <Input
            label="Email"
            value={email}
            onChangeText={(v) => {
              setEmail(v);
              setFieldErrors((prev) => ({ ...prev, email: undefined }));
            }}
            error={fieldErrors.email}
            keyboardType="email-address"
            autoComplete="email"
            placeholder="jan@przyklad.pl"
          />

          <Input
            label="Hasło"
            value={password}
            onChangeText={(v) => {
              setPassword(v);
              setFieldErrors((prev) => ({ ...prev, password: undefined }));
            }}
            error={fieldErrors.password}
            secureTextEntry
            autoComplete="password"
            placeholder="••••••••"
          />

          <Button
            label="Zaloguj się"
            onPress={handleLogin}
            loading={loading}
          />

          <TouchableOpacity
            style={styles.switchRow}
            onPress={() => navigation.navigate('Register')}
          >
            <Text style={styles.switchText}>
              Nie masz konta?{' '}
              <Text style={styles.switchLink}>Zarejestruj się</Text>
            </Text>
          </TouchableOpacity>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.background },
  container: {
    flexGrow: 1,
    justifyContent: 'center',
    paddingHorizontal: 24,
    paddingVertical: 40,
  },
  header: {
    alignItems: 'center',
    marginBottom: 40,
  },
  logo: { fontSize: 60, marginBottom: 12 },
  title: {
    fontSize: 26,
    fontWeight: '800',
    color: colors.textPrimary,
    marginBottom: 4,
  },
  subtitle: {
    fontSize: 15,
    color: colors.textSecondary,
  },
  form: {
    backgroundColor: colors.surface,
    borderRadius: 20,
    padding: 24,
    shadowColor: '#000',
    shadowOpacity: 0.06,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 4 },
    elevation: 3,
  },
  switchRow: {
    marginTop: 20,
    alignItems: 'center',
  },
  switchText: {
    fontSize: 14,
    color: colors.textSecondary,
  },
  switchLink: {
    color: colors.primary,
    fontWeight: '700',
  },
});
