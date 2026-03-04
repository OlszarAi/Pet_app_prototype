import React from 'react';
import {
  ActivityIndicator,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  TextInputProps,
  View,
} from 'react-native';

// ---- kolory ----

export const colors = {
  primary: '#4F6EF7',
  primaryDark: '#3B55D9',
  error: '#E53935',
  textPrimary: '#1A1A2E',
  textSecondary: '#666680',
  background: '#F5F7FF',
  surface: '#FFFFFF',
  border: '#D8DCF0',
  success: '#2E7D32',
} as const;

// ---- Input ----

interface InputProps extends TextInputProps {
  label: string;
  error?: string;
}

export function Input({ label, error, style, ...props }: InputProps) {
  return (
    <View style={styles.inputWrapper}>
      <Text style={styles.label}>{label}</Text>
      <TextInput
        style={[styles.input, error ? styles.inputError : null, style]}
        placeholderTextColor={colors.textSecondary}
        autoCapitalize="none"
        {...props}
      />
      {error ? <Text style={styles.errorText}>{error}</Text> : null}
    </View>
  );
}

// ---- Button ----

interface ButtonProps {
  label: string;
  onPress: () => void;
  loading?: boolean;
  disabled?: boolean;
  variant?: 'primary' | 'ghost';
}

export function Button({
  label,
  onPress,
  loading = false,
  disabled = false,
  variant = 'primary',
}: ButtonProps) {
  const isPrimary = variant === 'primary';
  return (
    <Pressable
      onPress={onPress}
      disabled={disabled || loading}
      style={({ pressed }) => [
        styles.button,
        isPrimary ? styles.buttonPrimary : styles.buttonGhost,
        (disabled || loading || pressed) && styles.buttonPressed,
      ]}
    >
      {loading ? (
        <ActivityIndicator color={isPrimary ? '#fff' : colors.primary} />
      ) : (
        <Text
          style={[
            styles.buttonLabel,
            isPrimary ? styles.buttonLabelPrimary : styles.buttonLabelGhost,
          ]}
        >
          {label}
        </Text>
      )}
    </Pressable>
  );
}

// ---- ErrorBanner ----

export function ErrorBanner({ message }: { message: string }) {
  return (
    <View style={styles.errorBanner}>
      <Text style={styles.errorBannerText}>{message}</Text>
    </View>
  );
}

// ---- styles ----

const styles = StyleSheet.create({
  inputWrapper: {
    marginBottom: 16,
  },
  label: {
    fontSize: 13,
    fontWeight: '600',
    color: colors.textPrimary,
    marginBottom: 6,
  },
  input: {
    height: 48,
    borderWidth: 1.5,
    borderColor: colors.border,
    borderRadius: 12,
    paddingHorizontal: 14,
    fontSize: 15,
    color: colors.textPrimary,
    backgroundColor: colors.surface,
  },
  inputError: {
    borderColor: colors.error,
  },
  errorText: {
    fontSize: 12,
    color: colors.error,
    marginTop: 4,
    marginLeft: 2,
  },
  button: {
    height: 52,
    borderRadius: 14,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 4,
  },
  buttonPrimary: {
    backgroundColor: colors.primary,
  },
  buttonGhost: {
    backgroundColor: 'transparent',
    borderWidth: 1.5,
    borderColor: colors.primary,
  },
  buttonPressed: {
    opacity: 0.7,
  },
  buttonLabel: {
    fontSize: 16,
    fontWeight: '700',
  },
  buttonLabelPrimary: {
    color: '#fff',
  },
  buttonLabelGhost: {
    color: colors.primary,
  },
  errorBanner: {
    backgroundColor: '#FFEBEE',
    borderRadius: 10,
    padding: 12,
    marginBottom: 16,
    borderLeftWidth: 4,
    borderLeftColor: colors.error,
  },
  errorBannerText: {
    color: colors.error,
    fontSize: 14,
  },
});
