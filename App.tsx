/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 */

import React from 'react';
import type {PropsWithChildren} from 'react';
import {
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  useColorScheme,
  View,
} from 'react-native';
import LoginScreen from './app/loginPage';
import RegisterScreen from './app/registrationPage';
import { NavigationContainer } from '@react-navigation/native';
import {createNativeStackNavigator} from '@react-navigation/native-stack';
import OtpScreen from './app/otpPage';
import HomeScreen from './app/homePage';
import AddThreadScreen from './app/addThreadPage';
import ThreadScreen from './app/threadPage';
import { Home, Search, Bell, Mail, CirclePlus, LucideAArrowDown, BanIcon} from "lucide-react-native";
import { createBottomTabNavigator } from "@react-navigation/bottom-tabs";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import WalletScreen from './app/walletPage';
import AddCardScreen from './app/addCardPage';


const queryClient = new QueryClient();



const LoginStack = createNativeStackNavigator();
const WalletStack = createNativeStackNavigator();
const HomeStack = createNativeStackNavigator();

const MainTab = createBottomTabNavigator();



function HomeStackNavigator() {
  return (
    <HomeStack.Navigator screenOptions={{ headerShown: false }}>
      <HomeStack.Screen name="Home_S" component={HomeScreen}  />
      <HomeStack.Screen name="Thread_S" component={ThreadScreen} />
      <HomeStack.Screen name="Wallet_S" component={WalletStackNavigator} />
    </HomeStack.Navigator>
  );
};

function WalletStackNavigator() {
  return (
    <WalletStack.Navigator screenOptions={{ headerShown: false }}>
      <HomeStack.Screen name="Wallet" component={WalletScreen} />
      <HomeStack.Screen name="Card_S" component={AddCardScreen} />
    </WalletStack.Navigator>
  );
};

function TabNavigator(){
    return(
      <MainTab.Navigator
          screenOptions={({ route }) => ({
            tabBarIcon: ({ focused, color, size }) => {

              if (route.name === "Home") return < Home size={size} color={focused ? "green" : "gray"} />;
              else if (route.name === "Search") return < Search size={size} color={focused ? "green" : "gray"} />;
              else if (route.name === "Add") return < CirclePlus size={size} color={focused ? "green" : "gray"} /> ;
              else if (route.name === "Notifications") return < Bell size={size} color={focused ? "green" : "gray"} />;
              else if (route.name === "Messages") return < Mail size={size} color={focused ? "green" : "gray"} />;

              return < BanIcon size={size} color={focused ? "green" : "gray"} />;
            },
            tabBarActiveTintColor: "green",
            tabBarInactiveTintColor: "gray",
            headerShown: false, // Hide header on all screens
          })}
        >
          <MainTab.Screen name="Home" component={HomeStackNavigator} />
          <MainTab.Screen name="Search" component={HomeScreen} />
          <MainTab.Screen name="Add" component={AddThreadScreen} />
          <MainTab.Screen name="Notifications" component={HomeScreen} />
          <MainTab.Screen name="Messages" component={HomeScreen} />
        </MainTab.Navigator>
    );
}










function App(): React.JSX.Element {


  return (

    
      <NavigationContainer>
       <QueryClientProvider client={queryClient}>
        <LoginStack.Navigator screenOptions={{ headerShown: false }}>
          <LoginStack.Screen
            name="Login"
            component={LoginScreen}
          />
          <LoginStack.Screen name="Register" component={RegisterScreen} />
          <LoginStack.Screen name="OTP" component={OtpScreen} />
          <LoginStack.Screen name="MainApp" component={TabNavigator} />
        </LoginStack.Navigator>  
       </QueryClientProvider>
      </NavigationContainer>
      

  
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#ff0000'
  },

  text: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333'
  }
})


export default App;
