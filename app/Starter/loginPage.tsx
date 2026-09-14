import React, { useState, useCallback } from "react";
import { View, StyleSheet, Alert, Keyboard } from "react-native";
import { TextInput, Button, Text } from "react-native-paper";
import { NativeStackScreenProps } from "@react-navigation/native-stack";
import { useFocusEffect } from "@react-navigation/native";
import axios, { Axios, AxiosError } from "axios";
import { IP_STRING } from "../Constants";
import { removeFBNToken, requestFBNPermission } from "../Components/FBCloudMessagingService";
import { clearUploadIdentity } from "../Components/FBAuthService";
import { LoginStore, screenStore } from "../GlobalFlags";
import {useNotificationListener } from "../Components/FBCloudMessagingService";
import { requestPasswordReset } from "../API";
import { getProfile } from "../API";
import { isPrivileged } from "../Roles";




const LoginScreen = ({navigation}:any) => {
  // "identifier" rather than "email": the server accepts either a username or an
  // address here, and calling it email is what made the screen disagree with
  // itself - the label asked for one thing and login matched on the other.
  const [identifier, setIdentifier] = useState("");
  const [password, setPassword] = useState("");

  useNotificationListener();


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
    } finally {
      // Uploads have their own Firebase session, derived from this one. Ending
      // ours without ending that leaves the next person on the device able to
      // upload as whoever was signed in last. In finally because that is most
      // true when the logout call itself failed.
      try {
        await clearUploadIdentity();
      } catch (e) {
        console.warn("Could not clear the upload identity:", e);
      }
    }
  };

  useFocusEffect(
    useCallback(() => {
      screenStore.set("Login");
      logout();
      LoginStore.set(false);
      //removeFBNToken();
    }, [])
  );


  const handleLogin = async () => {
    // Android resizes the whole activity window for the soft keyboard
    // (windowSoftInputMode=adjustResize), and this screen stays mounted
    // underneath the tabs after signing in. A password field still holding
    // focus therefore keeps the window short, and every flex:1 container in the
    // app is laid out into what is left - the feed appeared to stop halfway down
    // with the old keyboard area showing through beneath it.
    Keyboard.dismiss();

    try {
      // Credentials go in a form-encoded body, never the URL: query strings are
      // recorded in server access logs, proxy logs and crash reports. Values are
      // percent-encoded so passwords containing "&", "+" or "#" survive intact -
      // string concatenation into a URL truncated them at "&" and turned "+"
      // into a space.
      const body =
        `username=${encodeURIComponent(identifier)}` +
        `&password=${encodeURIComponent(password)}`;

      const response = await axios.post(IP_STRING + "/login", body, {
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
      });
      await requestFBNPermission();
      console.log("Login request sent!", response.status);
      LoginStore.set(true);
      // Store token for future API calls
      //await AsyncStorage.setItem("authToken", token);

      // Which interface to open depends on who signed in. The role comes from the
      // profile rather than the login response, so a session restored later lands
      // in the same place as a fresh sign-in.
      const profile = await getProfile();

      navigation.navigate(isPrivileged(profile) ? "AdminApp" : "MainApp");
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

  /**
   * Sends a reset link to the account named in the field above.
   *
   * Uses the field already on screen rather than opening a second one: somebody
   * who has just failed to sign in has usually typed their name already. It
   * takes either of the two things that field accepts, and the link always goes
   * to the account's own address - which is why this no longer says where it
   * went: when a username was typed, that address is not on screen to repeat.
   */
  const forgotPassword = async () => {
    if (!identifier.trim()) {
      Alert.alert("Your email or username first",
        "Type the one you signed up with, then tap this again.");
      return;
    }

    const answer = await requestPasswordReset(identifier.trim());
    if (answer) {
      // The server deliberately does not say whether the account exists, so
      // neither does this.
      Alert.alert("Check your email", answer);
    }
  };

  const toRegister =  () => {
    navigation.navigate("Register");
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Login</Text>
      <TextInput
        label="Email or username"
        value={identifier}
        onChangeText={setIdentifier}
        // Still the email keyboard: it puts @ and . on the first layer and
        // capitalises nothing, which suits both of the things this accepts.
        keyboardType="email-address"
        autoCapitalize="none"
        autoCorrect={false}
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
      {/* Somebody locked out cannot sign in to ask for a way to sign in, so this
          has to live on the screen they are stuck on. */}
      <Button style = {styles.rbutton} onPress={forgotPassword}>
        <Text style = {styles.special} >Forgot your password?</Text>
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
