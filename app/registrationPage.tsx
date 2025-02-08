import React, { useState } from "react";
import { View, StyleSheet,Alert } from "react-native";
import { TextInput, Button, Text } from "react-native-paper";
import { NativeStackScreenProps } from "@react-navigation/native-stack";
import axios, { Axios, AxiosError } from "axios";
import auth from '@react-native-firebase/auth';
import { IP_STRING } from "./Constants";



type RootStackParamList = {
    Login: undefined;
    Register: undefined;
  };
  
  type Props = NativeStackScreenProps<RootStackParamList, "Register">;

  const RegisterScreen = ({navigation}: any) => {
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPw, setConfirmPw] = useState("");
  const [user_name, setUsername] = useState("");
 
  async function verifyDetails() {

    const details = {
      "user_name":user_name,
      "pass_word":password,
      "phone_number":phone,
      "email":email,
    }
    
    
    try {
      const response = await axios.post(IP_STRING + "/req/check-details", details);
    } catch (error: any) {
      if (error.response) {
        Alert.alert("Error", error.response.data);
        return; // Stop execution if there's an error
      }
    }
      
    if(password != confirmPw){
      Alert.alert("Invalid Password","Passwords do not match")
      return;
    }

    const bundle = {
      "name" : user_name,
      "phone": phone,
      "email": email,
      "password": password, 
    }

    navigation.navigate("OTP",bundle);
  }



  return (
    <View style = {styles.container}>
      <View style = {styles.headerBlock}>
        <Text style={styles.header}>Register</Text>
        <Text style={styles.headerCaption}>Welcome! Create a free account with Stakes</Text>
      </View>
      <View style={styles.bodyBlock}>
      <Text>User name:</Text>
        <TextInput
          label="Enter your name"
          value={user_name}
          onChangeText={setUsername}
          keyboardType="default"
          autoCapitalize="none"
          style={styles.input}
        />
        <Text>Phone Number:</Text>
        <TextInput
          label="Enter Your Phone Number"
          value={phone}
          onChangeText={setPhone}
          keyboardType="phone-pad"
          style={styles.input}
        />
        <Text>Email:</Text>
        <TextInput
          label="Enter Your Email"
          value={email}
          onChangeText={setEmail}
          keyboardType="email-address"
          autoCapitalize="none"
          style={styles.input}
        />
        <Text>Password:</Text>
        <TextInput
          label="Enter Your Password"
          value={password}
          onChangeText={setPassword}
          secureTextEntry
          style={styles.input}
        />
        <TextInput
          label="Confirm Your Password"
          value={confirmPw}
          onChangeText={setConfirmPw}
          secureTextEntry
          style={styles.input}
        />
        <Button mode="contained" onPress={verifyDetails} style={styles.button}>
          Sign up!
        </Button>
        
        
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container:{
    flex: 1,
  },
  headerBlock:{
    flex: .3,
    backgroundColor: "#32cd32",
    justifyContent: "center"
  },
  header: {
    fontSize: 48,
    fontWeight: "bold",
    textAlign: "left",
    marginBottom: 20,
    paddingLeft: 10,
  },
  headerCaption: {
    fontSize: 20,
    fontWeight: "bold",
    textAlign: "left",
    marginBottom: 20,
    paddingLeft: 10,
  },
  bodyBlock: {
    flex: .7,
    justifyContent: "flex-start",
    padding: 20,
    backgroundColor: "#f6f2e6",
    paddingTop: 40,
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
    marginTop: 30,
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

export default RegisterScreen;

/*
axios.interceptors.response.use(
  response => response, // Pass through successful responses
  error => {
    if (error.response) {
      console.error(`❌ API Error: ${error.response.status}`, error.response.data);
    } else if (error.request) {
      console.error('⚠️ No Response:', error.request);
    } else {
      console.error('🚨 Request Error:', error.message);
    }
    return Promise.reject(error); // Ensure errors still propagate
  }
);
*/
