import { Stack } from 'expo-router';

export default function AuthLayout() {
  return (
    <Stack
      screenOptions={{
        headerShown: false,        // bez nagłówka — robimy własny design
        contentStyle: { backgroundColor: '#0a0a0a' },
      }}
    />
  );
}