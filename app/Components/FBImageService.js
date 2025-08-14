import { getStorage, ref, uploadBytes, getDownloadURL } from "firebase/storage";
import { initializeApp } from "firebase/app";
import uuid from 'react-native-uuid';
import { launchImageLibrary } from 'react-native-image-picker';



// Your Firebase config
const firebaseConfig = {
  apiKey: "AIzaSyDMmuBlfgJIiHfzCIMrZGOBmEHnM49lvao",
  authDomain: "betsocial-e7e93.firebaseapp.com",
  databaseURL: "https://betsocial-e7e93-default-rtdb.europe-west1.firebasedatabase.app/",
  projectId: "betsocial-e7e93",
  storageBucket: "betsocial-e7e93.firebasestorage.app",
  messagingSenderId: "707197061101",
  appId: "1:707197061101:android:10b58cafbedf35856e09e4",
};


const firebaseApp = initializeApp(firebaseConfig);
const storage = getStorage(firebaseApp);

export async function uploadImage(uri) {
  const response = await fetch(uri);
  const blob = await response.blob();

  const fileRef = ref(storage, `images/${uuid.v4()}.jpg`);
  await uploadBytes(fileRef, blob);
  return await getDownloadURL(fileRef); // public URL
}

export async function uploadPFP(uri,uid) {
  const response = await fetch(uri);
  const blob = await response.blob();

  const fileRef = ref(storage, `profile_pictures/${uid}.jpg`);
  await uploadBytes(fileRef, blob);
  return await getDownloadURL(fileRef); // public URL
}


export async function uploadVideo(uri) {
  const response = await fetch(uri);
  const blob = await response.blob();

  const fileRef = ref(storage, `videos/${uuid.v4()}.mp4`);
  await uploadBytes(fileRef, blob);
  return await getDownloadURL(fileRef); 
}


export const selectMedia = () => {
  return new Promise((resolve, reject) => {
    launchImageLibrary({ mediaType: 'mixed' }, response => {
      if (response.didCancel || response.errorCode) {
        console.warn('User cancelled or error:', response.errorMessage);
        reject('User cancelled or error');
        return;
      }

      const asset = response.assets?.[0];
      if (!asset) {
        reject('No media selected');
        return;
      }

      const mediaUri = asset.uri;
      const mediaType = asset.type?.startsWith('video') ? 'video' : 'image';

      if (mediaType === 'video') {
        uploadVideo(mediaUri)
          .then(url => {
            console.log("Video uploaded successfully:", url);
            resolve({ mediaUri: url, media_type: mediaType });
          })
          .catch(err => {
            console.error("Error uploading video:", err);
            reject(err);
          });
      } else {
        uploadImage(mediaUri)
          .then(url => {
            console.log("Image uploaded successfully:", url);
            resolve({ mediaUri: url, media_type: mediaType });
          })
          .catch(err => {
            console.error("Error uploading image:", err);
            reject(err);
          });
      }
    });
  });
};

export const selectLocalMedia = () => {
  return new Promise((resolve, reject) => {
    launchImageLibrary({ mediaType: 'mixed' }, response => {
      if (response.didCancel || response.errorCode) {
        console.warn('User cancelled or error:', response.errorMessage);
        reject('User cancelled or error');
        return;
      }

      const asset = response.assets?.[0];
      if (!asset) {
        reject('No media selected');
        return;
      }

      const mediaUri = asset.uri;
      const mediaType = asset.type?.startsWith('video') ? 'video' : 'image';

      resolve({ mediaUri: mediaUri, media_type: mediaType });     
    });
  });
};


export const selectImage = (profile) => {
  return new Promise((resolve, reject) => {
    launchImageLibrary({ mediaType: 'photo' }, response => {
      if (response.didCancel || response.errorCode) {
        console.warn('User cancelled or error:', response.errorMessage);
        reject('User cancelled or error');
        return;
      }

      const asset = response.assets?.[0];
      if (!asset) {
        reject('No media selected');
        return;
      }

      const mediaUri = asset.uri;
      if(!profile){
      uploadImage(mediaUri)
        .then(url => {
          console.log("Image uploaded successfully:", url);
          resolve(url);
        })
        .catch(err => {
          console.error("Error uploading image:", err);
          reject(err);
        });
      }else{
        uploadPFP(mediaUri, profile)
          .then(url => {
            console.log("Profile picture uploaded successfully:", url);
            resolve(url);
          })
          .catch(err => {
            console.error("Error uploading profile picture:", err);
            reject(err);
          });
      }
    });
  });
};


