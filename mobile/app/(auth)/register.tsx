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