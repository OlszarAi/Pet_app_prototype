import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useAuth } from '../context/AuthContext';
import { Button, colors } from '../components/ui';

export default function HomeScreen() {
  const { logout } = useAuth();

  return (
    <View style={styles.container}>
      <Text style={styles.icon}>🐾</Text>
      <Text style={styles.title}>PetsApp</Text>
      <Text style={styles.subtitle}>Jesteś zalogowany!</Text>
      <View style={styles.buttonWrapper}>
        <Button label="Wyloguj się" onPress={logout} variant="ghost" />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
  },
  icon: { fontSize: 72, marginBottom: 16 },
  title: {
    fontSize: 32,
    fontWeight: '800',
    color: colors.textPrimary,
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 16,
    color: colors.textSecondary,
    marginBottom: 40,
  },
  buttonWrapper: {
    width: '100%',
    maxWidth: 300,
  },
});
