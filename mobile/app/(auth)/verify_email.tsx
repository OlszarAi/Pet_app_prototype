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