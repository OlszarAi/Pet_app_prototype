import { Tabs } from 'expo-router';
import { useAuthStore } from '../../src/stores/authStore';
import { Redirect } from 'expo-router';

export default function TabsLayout() {
  const accessToken = useAuthStore((s) => s.accessToken);

  // Guard — niezalogowani nie mają tu wstępu
  if (!accessToken) {
    return <Redirect href="/(auth)/login" />;
  }

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarStyle: {
          backgroundColor: '#111',
          borderTopColor: '#222',
        },
        tabBarActiveTintColor: '#6C63FF',
        tabBarInactiveTintColor: '#666',
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: 'Strona główna',
          tabBarLabel: 'Dom',
        }}
      />
    </Tabs>
  );
}