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
import AddThreadScreen from './app/AddThreadPage';
import { Home, Search, Bell, Mail, CirclePlus, LucideAArrowDown, BanIcon } from "lucide-react-native";
import { createBottomTabNavigator } from "@react-navigation/bottom-tabs";


const Stack = createNativeStackNavigator();

const Tab = createBottomTabNavigator();

function TabNavigator(){
    return(
    <Tab.Navigator
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
       <Tab.Screen name="Home" component={HomeScreen} />
       <Tab.Screen name="Search" component={HomeScreen} />
       <Tab.Screen name="Add" component={AddThreadScreen} />
       <Tab.Screen name="Notifications" component={HomeScreen} />
       <Tab.Screen name="Messages" component={HomeScreen} />
     </Tab.Navigator>
    );
}

function App(): React.JSX.Element {


  return (

    
    <NavigationContainer>
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        <Stack.Screen
          name="Login"
          component={LoginScreen}
        />
        <Stack.Screen name="Register" component={RegisterScreen} />
        <Stack.Screen name="OTP" component={OtpScreen} />
        <Stack.Screen name="MainApp" component={TabNavigator} />
      </Stack.Navigator>  

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
