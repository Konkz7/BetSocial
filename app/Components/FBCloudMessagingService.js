import messaging from '@react-native-firebase/messaging';
import { saveFBN } from "../API";
import { useEffect } from 'react';
import { Alert } from 'react-native';


//const messaging = getMessaging(firebaseApp);

    

export const requestFBNPermission = async () => {
    const authStatus = await messaging().requestPermission();
    const enabled =
    authStatus === messaging.AuthorizationStatus.AUTHORIZED ||
    authStatus === messaging.AuthorizationStatus.PROVISIONAL;

  if (enabled) {
    console.log('Authorization status:', authStatus);

    // Get the device token
    const token = await messaging().getToken();
    console.log("FCM Token:", token);

    // Save to your backend
    await saveFBN(token);
  } else {
    console.log("Notification permission not granted.");
  }
};

export const useNotificationListener = () => {
  useEffect(() => {
    const unsubscribe = messaging().onMessage(async remoteMessage => {
      Alert.alert('New FCM Message!', JSON.stringify(remoteMessage));

    });

    return unsubscribe;
  }, []);
};




    
