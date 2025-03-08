import axios from "axios";
import { IP_STRING } from "./Constants";
import { Alert } from "react-native";

export const getProfile = async() =>{
    try {
        const profile = await axios.get(IP_STRING + "/req/profile");
        return (profile.data);
    } catch (error) {
      Alert.alert("Error!", "Profile couldnt be obtained.")
    }
  }

export const getCircleSecret = async () =>{
    try {
      const response = await axios.get(IP_STRING + "/circle/get-secret");
      return response.data;
    } catch (error) {
      Alert.alert("Error!", "Circle services are not secure/available: "+ error.message);
    }
  }

  export const getWallet = async () =>{
    try {
      const response = await axios.get(IP_STRING + "/circle/get-user-wallet");
      return response.data;
    } catch (error) {
      console.log("Error!", "Unable to find user wallet: "+ error.message);
    }
  }

  export const getBalance = async () =>{
    try {
      const response = await axios.get(IP_STRING + "/circle/get-balance");
      return response.data;
    } catch (error) {
      console.log("Error!", "Unable to find users' balance: "+ error.message);
    }
  }

  export const getIpAddress = async () =>{
    try {
      const response = await axios.get('https://api.ipify.org?format=json');
      return response.data;
    } catch (error) {
      console.log("Error!", "Unable to find users' balance: "+ error.message);
    }
  }