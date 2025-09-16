/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 */

import React, { useEffect, useState } from 'react';
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
import MessageScreen from './app/messagePage';
import SearchScreen from './app/searchPage';
import ProfileScreen from './app/profilePage';
import ActivityScreen from './app/activityPage';
import SelfProfileScreen from './app/selfProfilePage';
import SettingsScreen from './app/settingsPage';
import DMScreen from './app/dmPage';
import { activitySeenStore } from './app/GlobalFlags';


const queryClient = new QueryClient();



const LoginStack = createNativeStackNavigator();
const WalletStack = createNativeStackNavigator();
const SelfProfileStack = createNativeStackNavigator();
const HomeStack = createNativeStackNavigator();
const SearchStack = createNativeStackNavigator();
const ActivityStack = createNativeStackNavigator();
const MessageStack = createNativeStackNavigator();


const MainTab = createBottomTabNavigator();




function HomeStackNavigator() {
  return (
    <HomeStack.Navigator screenOptions={{ headerShown: false }}>
      <HomeStack.Screen name="Home_H" component={HomeScreen}  />
      <HomeStack.Screen name="Thread_H" component={ThreadScreen} />
      <HomeStack.Screen name="Wallet_H" component={WalletStackNavigator} />
      <HomeStack.Screen name="SelfProfile_H" component={SelfProfileStackNavigator} />
    </HomeStack.Navigator>
  );
};

function SearchStackNavigator() {
  return (
    <SearchStack.Navigator screenOptions={{ headerShown: false }}>
      <SearchStack.Screen name="Search_S" component={SearchScreen}  />
      <SearchStack.Screen name="Profile_S" component={ProfileScreen} />
      <SearchStack.Screen name="Thread_S" component={ThreadScreen} />
      <SearchStack.Screen name = "DMScreen_S" component ={DMScreen} />
    </SearchStack.Navigator>
  );
};

function SelfProfileStackNavigator() {
  return (
    <SelfProfileStack.Navigator screenOptions={{ headerShown: false }}>
      <SelfProfileStack.Screen name="SelfProfile_SP" component={SelfProfileScreen}  />
      <SelfProfileStack.Screen name="Thread_SP" component={ThreadScreen} />
      <SelfProfileStack.Screen name="Settings_SP" component={SettingsScreen} />
    </SelfProfileStack.Navigator>
  );
};

function ActivityStackNavigator() {
  return (
    <ActivityStack.Navigator screenOptions={{ headerShown: false }}>
      <ActivityStack.Screen name="Activity_A" component={ActivityScreen}  />
      <ActivityStack.Screen name="DMScreen_A" component={DMScreen}  />

    </ActivityStack.Navigator>
  );
};

function MessageStackNavigator() {
  return (
    <MessageStack.Navigator screenOptions={{ headerShown: false }}>
      <MessageStack.Screen name = "Messagescreen_M" component ={MessageScreen} />
      <MessageStack.Screen name = "DMScreen_M" component ={DMScreen} />

    </MessageStack.Navigator>
  );
};


function WalletStackNavigator() {
  return (
    <WalletStack.Navigator screenOptions={{ headerShown: false }}>
      <WalletStack.Screen name="Wallet" component={WalletScreen} />
      <WalletStack.Screen name="Card_W" component={AddCardScreen} />
    </WalletStack.Navigator>
  );
};

function TabNavigator(){
  const [seenActivity, setSeenActivity] = useState(activitySeenStore.get());

  useEffect(() => {
    // Patch your store's set method to notify React
    const originalSet = activitySeenStore.set;
    activitySeenStore.set = (value: boolean) => {
      originalSet(value);
      setSeenActivity(value); // trigger re-render
    };
  }, []);
    return(
      <MainTab.Navigator 
          screenOptions={({ route }) => ({
            tabBarIcon: ({ focused, color, size }) => {

              if (route.name === "Home") return < Home size={size} color={focused ? "green" : "gray"} />;
              else if (route.name === "Search") return < Search size={size} color={focused ? "green" : "gray"} />;
              else if (route.name === "Add") return < CirclePlus size={size} color={focused ? "green" : "gray"} /> ;
              else if (route.name === "Activity") 
                return <View>
                    <Bell size={size} color={focused ? "green" : "gray"} />
                    { !seenActivity && <View style = {{width:10,height:10,backgroundColor:'red',borderRadius:5,position:'absolute',top:-2,right:-10}}></View>}
                  </View>;
              else if (route.name === "Messages") return < Mail size={size} color={focused ? "green" : "gray"} />;

              return < BanIcon size={size} color={focused ? "green" : "gray"} />;
            },
            tabBarActiveTintColor: "green",
            tabBarInactiveTintColor: "gray",
            headerShown: false, // Hide header on all screens
          })}
        >
          <MainTab.Screen name="Home" component={HomeStackNavigator} />
          <MainTab.Screen name="Search" component={SearchStackNavigator} />
          <MainTab.Screen name="Add" component={AddThreadScreen} />
          <MainTab.Screen name="Activity" component={ActivityStackNavigator} />
          <MainTab.Screen name="Messages" component={MessageStackNavigator} />
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
