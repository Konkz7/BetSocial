
import { initializeApp } from "firebase/app";

export const IP_STRING = "http://192.168.1.53:8080"; 



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

