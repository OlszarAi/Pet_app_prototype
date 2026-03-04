import React from 'react';
import { ActivityIndicator, View } from 'react-native';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { useAuth } from '../context/AuthContext';
import { colors } from '../components/ui';
import LoginScreen from '../screens/LoginScreen';
import RegisterScreen from '../screens/RegisterScreen';
import VerifyEmailScreen from '../screens/VerifyEmailScreen';
import HomeScreen from '../screens/HomeScreen';
import { RootStackParamList } from './types';

const Stack = createNativeStackNavigator<RootStackParamList>();

export default function RootNavigator() {
  const { isAuthenticated, isLoading } = useAuth();

  // Splash – czeka na odczyt tokenów z SecureStore
  if (isLoading) {
    return (
      <View
        style={{ flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.background }}
      >
        <ActivityIndicator size="large" color={colors.primary} />
      </View>
    );
  }

  return (
    <NavigationContainer>
      <Stack.Navigator
        screenOptions={{
          headerShown: false,
          animation: 'slide_from_right',
          contentStyle: { backgroundColor: colors.background },
        }}
      >
        {isAuthenticated ? (
          // ---- Ekrany zalogowanego użytkownika ----
          <Stack.Screen name="Home" component={HomeScreen} />
        ) : (
          // ---- Ekrany auth ----
          <>
            <Stack.Screen name="Login" component={LoginScreen} />
            <Stack.Screen name="Register" component={RegisterScreen} />
            <Stack.Screen
              name="VerifyEmail"
              component={VerifyEmailScreen}
              options={{ gestureEnabled: false }}
            />
          </>
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
}
