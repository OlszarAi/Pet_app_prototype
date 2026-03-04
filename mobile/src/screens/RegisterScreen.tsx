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
import { Button, colors, ErrorBanner, Input } from '../components/ui';
import { RootStackParamList } from '../navigation/types';

type Props = NativeStackScreenProps<RootStackParamList, 'Register'>;

export default function RegisterScreen({ navigation }: Props) {
  const [email, setEmail] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const [fieldErrors, setFieldErrors] = useState<{
    email?: string;
    username?: string;
    password?: string;
    confirm?: string;
  }>({});

  function validate(): boolean {
    const errs: typeof fieldErrors = {};
    if (!email.trim()) errs.email = 'Email jest wymagany';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email))
      errs.email = 'Nieprawidłowy format email';

    if (!username.trim()) errs.username = 'Nazwa użytkownika jest wymagana';
    else if (username.trim().length < 3)
      errs.username = 'Minimum 3 znaki';
    else if (username.trim().length > 30)
      errs.username = 'Maksimum 30 znaków';
    else if (!/^[a-zA-Z0-9_]+$/.test(username.trim()))
      errs.username = 'Tylko litery, cyfry i podkreślnik (_)';

    if (!password) errs.password = 'Hasło jest wymagane';
    else if (password.length < 8) errs.password = 'Minimum 8 znaków';
    else if (!/[A-Z]/.test(password)) errs.password = 'Wymagana co najmniej jedna wielka litera';
    else if (!/[0-9]/.test(password)) errs.password = 'Wymagana co najmniej jedna cyfra';

    if (!confirm) errs.confirm = 'Potwierdź hasło';
    else if (confirm !== password) errs.confirm = 'Hasła nie są zgodne';

    setFieldErrors(errs);
    return Object.keys(errs).length === 0;
  }

  function clearField(field: keyof typeof fieldErrors) {
    setFieldErrors((prev) => ({ ...prev, [field]: undefined }));
  }

  async function handleRegister() {
    if (!validate()) return;
    setError('');
    setLoading(true);
    try {
      await authApi.register({
        email: email.trim(),
        username: username.trim(),
        password,
      });
      // Przekieruj na ekran weryfikacji emaila, przekazując email
      navigation.navigate('VerifyEmail', { email: email.trim() });
    } catch (e) {
      if (e instanceof ApiError) {
        if (e.status === 409)
          setError('Konto z tym emailem lub nazwą użytkownika już istnieje.');
        else if (e.status === 429)
          setError('Zbyt wiele prób rejestracji. Poczekaj chwilę i spróbuj ponownie.');
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
          <Text style={styles.title}>Utwórz konto</Text>
          <Text style={styles.subtitle}>Dołącz do społeczności PetsApp</Text>
        </View>

        <View style={styles.form}>
          {error ? <ErrorBanner message={error} /> : null}

          <Input
            label="Email"
            value={email}
            onChangeText={(v) => { setEmail(v); clearField('email'); }}
            error={fieldErrors.email}
            keyboardType="email-address"
            autoComplete="email"
            placeholder="jan@przyklad.pl"
          />

          <Input
            label="Nazwa użytkownika"
            value={username}
            onChangeText={(v) => { setUsername(v); clearField('username'); }}
            error={fieldErrors.username}
            autoComplete="username"
            placeholder="jan_kowalski"
          />

          <Input
            label="Hasło"
            value={password}
            onChangeText={(v) => { setPassword(v); clearField('password'); }}
            error={fieldErrors.password}
            secureTextEntry
            autoComplete="new-password"
            placeholder="Min. 8 znaków, wielka litera, cyfra"
          />

          <Input
            label="Potwierdź hasło"
            value={confirm}
            onChangeText={(v) => { setConfirm(v); clearField('confirm'); }}
            error={fieldErrors.confirm}
            secureTextEntry
            autoComplete="new-password"
            placeholder="••••••••"
          />

          <Button
            label="Zarejestruj się"
            onPress={handleRegister}
            loading={loading}
          />

          <TouchableOpacity
            style={styles.switchRow}
            onPress={() => navigation.navigate('Login')}
          >
            <Text style={styles.switchText}>
              Masz już konto?{' '}
              <Text style={styles.switchLink}>Zaloguj się</Text>
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
