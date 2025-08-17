/**
 * @format
 */

import {AppRegistry} from 'react-native';
import App from './App';
import {name as appName} from './app.json';
import { firebase } from '@react-native-firebase/app';
import messaging from '@react-native-firebase/messaging';

  


AppRegistry.registerComponent(appName, () => App);

// Register background handler
messaging().setBackgroundMessageHandler(async remoteMessage => {
console.log('Message handled in the background!', remoteMessage);
});