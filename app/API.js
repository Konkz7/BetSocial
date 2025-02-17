import axios from "axios";
import { IP_STRING } from "./Constants";
import { Alert } from "react-native";

const USER_URL = IP_STRING + "/api/users"; // Replace with your actual API


export const getProfile = async() =>{
    try {
        const profile = await axios.get(IP_STRING + "/req/profile");
        return (profile.data);
    } catch (error) {
      Alert.alert("Error!", "Profile couldnt be obtained.")
    }
  }