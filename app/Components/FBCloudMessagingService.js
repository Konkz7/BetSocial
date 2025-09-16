import messaging from '@react-native-firebase/messaging';
import { saveFBN } from "../API";
import { useEffect } from 'react';
import { Alert } from 'react-native';
import { activitySeenStore, screenStore } from '../GlobalFlags';
import { eventEmitter } from './EventBus';



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
      handleIncomingNotification(remoteMessage);
    });

    return unsubscribe;
  }, []);
};


function handleIncomingNotification(remoteMessage) {
  if (screenStore.get() === 'Activity') {   

    console.log('Refreshing Activity Screen...');
    eventEmitter.emit('notificationReceived', remoteMessage.data);

  } else if (screenStore.get() === 'Message') {  

    console.log('Refreshing Messages Screen...');
    eventEmitter.emit('notificationReceived', remoteMessage.data);
    if(remoteMessage.data?.type !== "message"){
      activitySeenStore.set(false);
    }

  } else if (screenStore.get() === 'Chat') {   
 
    eventEmitter.emit('notificationReceived', remoteMessage.data);
    if(remoteMessage.data?.type !== "message"){
      activitySeenStore.set(false);
    }

  }else {
    // Show normal notification UI (banner, toast, etc.)
    // mark notifications as unseen
    activitySeenStore.set(false);
    console.log('Not on chat screen, maybe show push banner');
  }
}



    
