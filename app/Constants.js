
import axios from "axios";
import { Alert, Platform } from "react-native";
import { initializeApp } from "firebase/app";
import {firebaseConfig, devApiUrl} from "./Secrets";

/**
 * Where the app talks to.
 *
 * This used to be one hardcoded LAN address, edited by hand whenever the machine
 * moved network and committed by accident when it did not. Two things were wrong
 * with that beyond the nuisance: a release build could ship pointing at a
 * laptop, and there was nowhere to put a deployed URL that did not also have to
 * be reverted before the next day's development.
 *
 * So the two are separate values and the build picks. Nothing to remember, and a
 * release cannot accidentally be a development build.
 */

/**
 * The deployed server. Tracked, because it is not a secret - it is the address
 * printed on the app.
 */
const PRODUCTION_API = "https://betsocial.example.org";

/**
 * This machine, for development.
 *
 * Lives in Secrets.js because it is per-machine and per-network, and that file
 * is already the gitignored one - so changing networks stops being an edit to a
 * tracked file that then wants committing.
 *
 * The fallback differs by platform because the two simulators reach their host
 * differently: 10.0.2.2 is the Android emulator's route to it, while the iOS
 * simulator shares the host's network and so just uses localhost. Neither works
 * on a physical device, which is what the warning below is for.
 */
const SIMULATOR_HOST = Platform.OS === "android"
  ? "http://10.0.2.2:8080"
  : "http://localhost:8080";

const DEVELOPMENT_API = devApiUrl || SIMULATOR_HOST;

export const IP_STRING = __DEV__ ? DEVELOPMENT_API : PRODUCTION_API;

/**
 * The chat socket, derived rather than written out again.
 *
 * `^http` -> `ws` turns http into ws and https into wss in one step, which
 * matters: a deployed server is https, and pairing it with a plain ws:// socket
 * fails in a way that looks like the socket problem rather than a URL problem.
 * The session cookie is Secure in production, so it would not be sent over ws://
 * even if the connection opened.
 */
export const WS_URL = IP_STRING.replace(/^http/, "ws") + "/ws";

if (__DEV__ && !devApiUrl) {
  console.error(
    "devApiUrl is not set in app/Secrets.js, so the app is pointed at " +
    DEVELOPMENT_API + " - this simulator's route to its host. On a physical " +
    "device nothing will load. Add devApiUrl with this machine's LAN address, " +
    "e.g. \"http://192.168.1.210:8080\". See app/Secrets.example.js."
  );
}

if (!__DEV__ && PRODUCTION_API.includes("example.org")) {
  console.error(
    "PRODUCTION_API in app/Constants.js is still the placeholder. This is a " +
    "release build and it is pointed at nothing."
  );
}



//Firebase app instance
export const firebaseApp = initializeApp(firebaseConfig);



export const errorHandler = (error) => {
    
    if (axios.isAxiosError(error)) {
        console.error("Axios error:", error.response?.data);
        Alert.alert("Operation Failed", JSON.stringify(error.response?.data) || "Invalid credentials.");
      } else {
        console.error("Unexpected error:", error.message);
        Alert.alert("Error", "Something went wrong.");
      }
}

export const timeAgo = (timestamp) => {
  const now = Date.now();
  const diffMs = now - timestamp; // Difference in milliseconds

  const seconds = Math.floor(diffMs / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);
  const weeks = Math.floor(days / 7);
  const months = Math.floor(days / 30);
  const years = Math.floor(days / 365);

  if (seconds < 60) return `${seconds}s`;
  if (minutes < 60) return `${minutes}m`;
  if (hours < 24) return `${hours}h`;
  if (days < 7) return `${days}d`;
  if (weeks < 4) return `${weeks}w`;
  if (months < 12) return `${months}M`;
  return `${years}y`;
};

export function formatMessageTime(created_at) {
  const createdTime = new Date(created_at).getTime();
  const now = Date.now();
  return now - createdTime > 86400000
    ? new Date(created_at).toLocaleDateString([], { day: '2-digit', month: '2-digit', year: '2-digit' })
    : new Date(created_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

export const getProfilePictureUrl = (url) => {
  return url  ? {uri : url} : require('../app/assets/icon.png');
}

