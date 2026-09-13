/**
 * @format
 */

// First, before anything that might need them. React Native provides no
// TextEncoder or TextDecoder, and stompjs constructs one while setting up the
// chat socket - see the file for what that failure looked like.
import './app/Components/TextEncodingPolyfill';

import {AppRegistry} from 'react-native';
import App from './App';
import {name as appName} from './app.json';
import { firebase } from '@react-native-firebase/app';
import messaging from '@react-native-firebase/messaging';

  
// Register background handler
messaging().setBackgroundMessageHandler(async remoteMessage => {
console.log('Message handled in the background!', remoteMessage);
});

AppRegistry.registerComponent(appName, () => App);

 