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