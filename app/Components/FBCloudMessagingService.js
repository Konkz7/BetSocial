import messaging from '@react-native-firebase/messaging';
import { saveFBN } from "../API";
import { useEffect, useContext } from 'react';
import { Alert } from 'react-native';
import { activitySeenStore, messageSeenStore, screenStore } from '../GlobalFlags';
import { eventEmitter } from './EventBus';
import { BannerContext } from './BannerProvider';
import { useNavigation } from '@react-navigation/native';

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

export const removeFBNToken = async() =>{
   await messaging().deleteToken();
}

export const useNotificationListener = () => {
  const { showBanner } = useContext(BannerContext);
  const navigation = useNavigation();

  useEffect(() => {
    const handleIncomingNotification = (remoteMessage) => {
      console.log("handleIncomingNotification called!");
      console.log("Notification type: ", remoteMessage.data?.type);

      

      if (remoteMessage.data?.type === "message") {
        messageSeenStore.set(false);
      }

      if (screenStore.get() !== 'Activity') {  
        activitySeenStore.set(false);
      }

      // ✅ you can now safely use context here
      showBanner?.({
        title: remoteMessage.notification?.title,
        body: remoteMessage.notification?.body,
      });

      if (screenStore.get() === 'Activity') {
        console.log('Refreshing Activity Screen...');
        eventEmitter.emit('notificationReceived', remoteMessage.data);
      } else if (screenStore.get() === 'Message') {
        console.log('Refreshing Messages Screen...');
        eventEmitter.emit('notificationReceived', remoteMessage.data);
      } else if (screenStore.get() === 'Chat') {
        eventEmitter.emit('notificationReceived', remoteMessage.data);
      } else {
        console.log('Not on chat screen, maybe show push banner');
      }
    };

    const unsubscribe = messaging().onMessage(async (remoteMessage) => {
      //Alert.alert('New FCM Message!', JSON.stringify(remoteMessage));
      handleIncomingNotification(remoteMessage);
    });


    const unsubscribeNotificationOpened = messaging().onNotificationOpenedApp(remoteMessage => {
      console.log('Notification opened from background state:', remoteMessage);
      navigateFromNotification(remoteMessage, navigation);
    });

    // When app is opened from a quit state
    messaging()
      .getInitialNotification()
      .then(remoteMessage => {
        if (remoteMessage) {
          console.log('Notification caused app to open from quit state:', remoteMessage);
          navigateFromNotification(remoteMessage, navigation);
        }
      });

    return () => {
      unsubscribe();
      unsubscribeNotificationOpened();
    };
  }, [showBanner,navigation]);
};

async function goToThreadScreen(tid , navigation){
    
  const profile = await toThreadProfile(tid);

  navigation.navigate("Home", {
      screen: "Thread_H",
      params: profile, 
  });

}

const navigateFromNotification = (remoteMessage, navigation) => {
  const type = remoteMessage.data?.type;
  if (type === 'message') {
    navigation.navigate('Message');
  } else if (type === 'new_thread') {
    goToThreadScreen(remoteMessage.data?.target_id,navigation);
  } else {
    navigation.navigate('Activity');
  }
};
