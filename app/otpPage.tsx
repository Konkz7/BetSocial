import React, { useState } from "react";
import { View, StyleSheet,Alert } from "react-native";
import { TextInput, Button, Text } from "react-native-paper";
import { NativeStackScreenProps } from "@react-navigation/native-stack";
import axios, { Axios, AxiosError } from "axios";
import auth from '@react-native-firebase/auth';
import { IP_STRING } from "./Constants";



const OtpScreen = ({navigation,route}: any) => {
    
    const [confirmation, setConfirmation] = useState<any>(null);
    const [otpCode, setOtp] = useState("");
    const bundle = route.params;


    // 2️⃣ Function to verify OTP and send the token to backend
    async function verifyOTP() {
        try {
            if (!confirmation) {
                Alert.alert("Error", "Please request an OTP first.");
                return;
            }
            
            const userCredential = await confirmation.confirm(otpCode); // Verify OTP
            const idToken = await userCredential.user.getIdToken(); // Get Firebase ID token

            console.log("Waiting for backend");

            // Send token to backend for verification
            const response = await axios.post(IP_STRING+"/req/phone-verification?idToken="+idToken);

            console.log("Server Response:", response.data);
            Alert.alert("Success", "Phone number verified successfully!");
            sendEmail();
            navigation.reset({
              index: 0,
              routes: [{ name: 'Login' }], // Replace with your first screen name
            });
        } catch (error) {
            console.error("Error verifying OTP:", error);
            Alert.alert("Error", "Failed to verify OTP. Please check and try again.");
        }
    }

    // 1️⃣ Function to send OTP to the phone number
    async function sendOTP() {
        try {
            const confirmationResult = await auth().signInWithPhoneNumber(bundle.phone);
            setConfirmation(confirmationResult);
            Alert.alert("OTP Sent", "Please check your messages for the OTP.");
            
        } catch (error) {
            console.error("Error sending OTP:", error);
            Alert.alert("Error", "Failed to send OTP. Please try again.");
        }
      }

    
    async function sendEmail() {
        try {
            const details ={
                "user_name": bundle.name,             
                "pass_word": bundle.password,
                "email": bundle.email,
                "phone_number":bundle.phone,
            }
            const register = await axios.post(IP_STRING + "/req/register",details);
            Alert.alert("Email Sent", "Please check your email for the verification link.");
        } catch (error) {
            console.error("Error sending email:", error);
            Alert.alert("Error", "Failed to send email. Please try again.");
        }
    }
    

    function test(){
        console.log(bundle.email);
    }

    return (
        <View style = {styles.container}>
        <View style = {styles.headerBlock}>
            <Text style={styles.header}>Register</Text>
            <Text style={styles.headerCaption}>Welcome! Create a free account with Stakes</Text>
        </View>
        <View style={styles.bodyBlock}>
            
            <TextInput
                    label="Enter OTP"
                    value={otpCode}
                    onChangeText={setOtp}
                    keyboardType="number-pad"
                    style={styles.input}
            />
            <Button mode="contained" onPress={sendOTP} style={styles.button}>
            Send OTP
            </Button>
            <Button mode="contained" onPress={verifyOTP} style={styles.button}>
            Verify!
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

export default OtpScreen;
