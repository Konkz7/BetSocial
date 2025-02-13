import React, { useState, useCallback } from "react";
import { View, StyleSheet,Alert } from "react-native";
import { TextInput, Button, Text } from "react-native-paper";
import { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useFocusEffect } from "@react-navigation/native";
import axios, { Axios, AxiosError } from "axios";
import { IP_STRING } from "./Constants";



type RootStackParamList = {
    Login: undefined;
    Register: undefined;
  };
  
  type Props = NativeStackScreenProps<RootStackParamList, "Login">;

const LoginScreen = ({navigation}:any) => {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const logout = async () => {
    try{
      const response = await axios.post(IP_STRING + "/logout")
      console.log("Logout successful!", response.data);
    } catch (error) {
      if (axios.isAxiosError(error)) {
        console.error("Axios error:", error.response?.data);
        Alert.alert("Logout Failed", error.response?.data || "Invalid credentials.");
      } else {
        console.error("Unexpected error:", (error as Error).message);
        Alert.alert("Error", "Something went wrong.");
      }
    }
  };

  useFocusEffect(
    useCallback(() => {
      console.log("Screen is focused! Perform refresh or action here.");  
      logout();
      return () => {
        console.log("Screen is unfocused! Cleanup if needed.");
      };
    }, [])
  );


  const handleLogin = async () => {
    try {
      const response = await axios.post(IP_STRING + "/login?username="+email+"&password="+password);
      console.log("Login request sent!", response.data);
      console.log("Login request sent!", response.status);

      // Store token for future API calls
      //await AsyncStorage.setItem("authToken", token);

      // Navigate to Home screen
      navigation.navigate("MainApp");
    } catch (error) {
      if (axios.isAxiosError(error)) {
        console.error("Axios error:", error.response?.data);
        Alert.alert("Login Failed", JSON.stringify(error.response?.data) || "Invalid credentials.");
      } else {
        console.error("Unexpected error:", (error as Error).message);
        Alert.alert("Error", "Something went wrong.");
      }
    }
  };

  const toRegister =  () => {
    navigation.navigate("Register");
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Login</Text>
      <TextInput
        label="Email"
        value={email}
        onChangeText={setEmail}
        keyboardType="email-address"
        autoCapitalize="none"
        style={styles.input}
      />
      <TextInput
        label="Password"
        value={password}
        onChangeText={setPassword}
        secureTextEntry
        style={styles.input}
      />
      <Button mode="contained" onPress={handleLogin} style={styles.button}>
        Login
      </Button>
      <Button style = {styles.rbutton} onPress={toRegister}>
        <Text style = {styles.special} >Don't have an account? Sign Up</Text>
      </Button>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: "center",
    padding: 20,
    backgroundColor: "#f6f2e6"
  },
  title: {
    fontSize: 24,
    fontWeight: "bold",
    textAlign: "left",
    marginBottom: 20,
  },
  input: {
    marginBottom: 10,
    backgroundColor: "#D1FDCC"
  },
  button: {
    marginTop: 10,
    backgroundColor: "#32cd32"
  },
  rbutton:{
    marginTop: 20,
  },
  special:{
    color: "#03FB52",
    fontWeight: "bold"
  }
});

export default LoginScreen;
