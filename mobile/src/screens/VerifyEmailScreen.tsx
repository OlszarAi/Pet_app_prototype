import React, { useEffect, useRef, useState } from 'react';
import {
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { authApi } from '../api/auth';
import { ApiError } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { Button, colors, ErrorBanner } from '../components/ui';
import { RootStackParamList } from '../navigation/types';

type Props = NativeStackScreenProps<RootStackParamList, 'VerifyEmail'>;

// Czas oczekiwania (sekundy) na ponowne wysłanie kodu
const RESEND_COOLDOWN = 60;

export default function VerifyEmailScreen({ navigation, route }: Props) {
  const { email } = route.params;
  const { saveTokens } = useAuth();

  // Sześć osobnych pól dla każdej cyfry kodu
  const [digits, setDigits] = useState<string[]>(Array(6).fill(''));
  const inputs = useRef<(TextInput | null)[]>(Array(6).fill(null));

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // Odliczanie cooldown'u przycisku "Wyślij ponownie"
  const [cooldown, setCooldown] = useState(0);

  useEffect(() => {
    if (cooldown <= 0) return;
    const id = setTimeout(() => setCooldown((c) => c - 1), 1000);
    return () => clearTimeout(id);
  }, [cooldown]);

  function handleDigitChange(index: number, value: string) {
    // Obsługa wklejenia całego kodu (np. ze schowka)
    if (value.length === 6 && /^\d{6}$/.test(value)) {
      const arr = value.split('');
      setDigits(arr);
      inputs.current[5]?.focus();
      return;
    }

    // Obsługa pojedynczej cyfry
    const digit = value.replace(/\D/g, '').slice(-1);
    const updated = [...digits];
    updated[index] = digit;
    setDigits(updated);

    if (digit && index < 5) {
      inputs.current[index + 1]?.focus();
    }
  }

  function handleKeyPress(index: number, key: string) {
    if (key === 'Backspace' && !digits[index] && index > 0) {
      inputs.current[index - 1]?.focus();
    }
  }

  async function handleVerify() {
    const code = digits.join('');
    if (code.length < 6) {
      setError('Wpisz pełny 6-cyfrowy kod weryfikacyjny.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      const tokens = await authApi.verifyEmail({ email, code });
      await saveTokens(tokens);
      // Po pomyślnej weryfikacji AuthProvider automatycznie przełączy do ekranu głównego
    } catch (e) {
      if (e instanceof ApiError) {
        if (e.status === 400 || e.status === 401)
          setError('Nieprawidłowy lub przeterminowany kod. Sprawdź email i spróbuj ponownie.');
        else
          setError(e.message);
      } else {
        setError('Nie można połączyć się z serwerem. Sprawdź internet.');
      }
    } finally {
      setLoading(false);
    }
  }

  async function handleResend() {
    if (cooldown > 0) return;
    setError('');
    setSuccessMsg('');
    try {
      await authApi.resendVerification({ email });
      setSuccessMsg('Nowy kod został wysłany na Twój adres email.');
      setCooldown(RESEND_COOLDOWN);
      setDigits(Array(6).fill(''));
      inputs.current[0]?.focus();
    } catch (e) {
      if (e instanceof ApiError && e.status === 429)
        setError('Zbyt wiele prób. Poczekaj chwilę przed ponownym wysłaniem.');
      else
        setError('Nie udało się wysłać kodu. Spróbuj ponownie.');
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
          <Text style={styles.icon}>📧</Text>
          <Text style={styles.title}>Potwierdź email</Text>
          <Text style={styles.subtitle}>
            Wysłaliśmy 6-cyfrowy kod na{'\n'}
            <Text style={styles.emailBold}>{email}</Text>
          </Text>
        </View>

        <View style={styles.form}>
          {error ? <ErrorBanner message={error} /> : null}
          {successMsg ? (
            <View style={styles.successBanner}>
              <Text style={styles.successText}>{successMsg}</Text>
            </View>
          ) : null}

          {/* Pola kodu */}
          <View style={styles.codeRow}>
            {digits.map((d, i) => (
              <TextInput
                key={i}
                ref={(ref) => { inputs.current[i] = ref; }}
                style={[styles.digitInput, d ? styles.digitFilled : null]}
                value={d}
                onChangeText={(v) => handleDigitChange(i, v)}
                onKeyPress={({ nativeEvent }) =>
                  handleKeyPress(i, nativeEvent.key)
                }
                keyboardType="number-pad"
                maxLength={6}
                selectTextOnFocus
                textAlign="center"
              />
            ))}
          </View>

          <Button
            label="Zweryfikuj konto"
            onPress={handleVerify}
            loading={loading}
            disabled={digits.join('').length < 6}
          />

          <TouchableOpacity
            style={styles.resendRow}
            onPress={handleResend}
            disabled={cooldown > 0}
          >
            <Text
              style={[
                styles.resendText,
                cooldown > 0 ? styles.resendDisabled : null,
              ]}
            >
              {cooldown > 0
                ? `Wyślij ponownie za ${cooldown}s`
                : 'Nie dostałeś kodu? Wyślij ponownie'}
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.backRow}
            onPress={() => navigation.navigate('Login')}
          >
            <Text style={styles.backText}>← Wróć do logowania</Text>
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
  icon: { fontSize: 60, marginBottom: 12 },
  title: {
    fontSize: 26,
    fontWeight: '800',
    color: colors.textPrimary,
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 15,
    color: colors.textSecondary,
    textAlign: 'center',
    lineHeight: 22,
  },
  emailBold: {
    fontWeight: '700',
    color: colors.textPrimary,
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
  codeRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 24,
    gap: 8,
  },
  digitInput: {
    flex: 1,
    height: 56,
    borderWidth: 1.5,
    borderColor: colors.border,
    borderRadius: 12,
    fontSize: 22,
    fontWeight: '700',
    color: colors.textPrimary,
    backgroundColor: colors.background,
  },
  digitFilled: {
    borderColor: colors.primary,
    backgroundColor: '#EEF1FF',
  },
  resendRow: {
    marginTop: 20,
    alignItems: 'center',
  },
  resendText: {
    fontSize: 14,
    color: colors.primary,
    fontWeight: '600',
  },
  resendDisabled: {
    color: colors.textSecondary,
  },
  backRow: {
    marginTop: 12,
    alignItems: 'center',
  },
  backText: {
    fontSize: 13,
    color: colors.textSecondary,
  },
  successBanner: {
    backgroundColor: '#E8F5E9',
    borderRadius: 10,
    padding: 12,
    marginBottom: 16,
    borderLeftWidth: 4,
    borderLeftColor: colors.success,
  },
  successText: {
    color: colors.success,
    fontSize: 14,
  },
});
