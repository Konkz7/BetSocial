import { getStorage, ref, uploadBytes, getDownloadURL } from "firebase/storage";
import { initializeApp } from "firebase/app";
import uuid from 'react-native-uuid';
import { launchImageLibrary } from 'react-native-image-picker';
import {firebaseApp} from "../Constants";
import { firebaseConfig } from "../Secrets";
import { withUploadIdentity } from "./FBAuthService";
import { Image, Video } from 'react-native-compressor';
import RNFS from "react-native-fs";

// Firebase reports both of these the same way - "No default bucket found" - which
// says nothing about which of the two happened, and the screens turn it into
// "Apologies! Image couldnt be Uploaded" before anyone sees the cause.
//
// This does not throw. A bad config here should not take down a running app, and
// the upload path already has an error to show; the point is to leave something
// in the log that names the actual problem.
if (firebaseConfig?.type === "service_account" || firebaseConfig?.private_key) {
  // The service-account JSON and the web app config both come from the Firebase
  // console and both look like "the Firebase credentials", but only one belongs
  // in the app. This file is bundled, so a service account pasted here ships a
  // private key with full project access to every device that installs it.
  console.error(
    "app/Secrets.js contains a service-account key. That one belongs to the " +
    "backend (BetSocial/src/main/resources/firebaseAPI.json) and must never be " +
    "bundled into the app - rotate it if this has been built or shared. The app " +
    "needs the web app config instead: Firebase console -> Project settings -> " +
    "General -> Your apps -> SDK setup and configuration. See app/Secrets.example.js."
  );
} else if (!firebaseConfig?.storageBucket) {
  console.error(
    "firebaseConfig.storageBucket is missing from app/Secrets.js, so uploads " +
    "cannot work. It is the gs:// name shown in the Firebase console under " +
    "Storage, written without the gs:// prefix."
  );
}

// Uploads carry an identity, which is what lets the Storage rules ask who is
// writing rather than only how big the file is. See FBAuthService: there are two
// Firebase SDKs in this app with separate auth state, and this file uses the JS
// one, which nothing else signs into.
const storage = getStorage(firebaseApp);

const IMAGE_SOFT_LIMIT = 3 * 1024 * 1024;   // 3MB
const IMAGE_HARD_LIMIT = 15 * 1024 * 1024;  // 15MB
const VIDEO_SOFT_LIMIT = 50 * 1024 * 1024;  // 50MB
const VIDEO_HARD_LIMIT = 100 * 1024 * 1024; // 100MB

const getFileSize = async (uri) => {
  const stat = await RNFS.stat(uri);
  return stat.size;
};

export async function uploadImage(uri) {
  const fileSize = await getFileSize(uri);

   let compressedUri = uri;

  if (fileSize > IMAGE_HARD_LIMIT) {
    throw new Error("Image too large (max 15MB). Please choose a smaller file.");
  }

  if (fileSize > IMAGE_SOFT_LIMIT) {
    console.log("Compressing image before upload...");
    compressedUri = await Image.compress(uri, { quality: 0.7 });
  }

  const response = await fetch(compressedUri);
  const blob = await response.blob();

  const fileRef = ref(storage, `images/${uuid.v4()}.jpg`);
  return withUploadIdentity(async () => {
    await uploadBytes(fileRef, blob);
    return await getDownloadURL(fileRef); // public URL
  });
}

export async function uploadPFP(uri,uid) {
  const fileSize = await getFileSize(uri);
  let compressedUri = uri;

  if (fileSize > IMAGE_HARD_LIMIT) {
    throw new Error("Profile picture too large (max 15MB).");
  }

  if (fileSize > IMAGE_SOFT_LIMIT) {
    console.log("Compressing profile picture...");
    compressedUri = await Image.compress(uri, { quality: 0.7 });
  }

  const response = await fetch(compressedUri);
  const blob = await response.blob();

  // The name is the uid on purpose: it is what lets the rules require that the
  // person writing profile_pictures/7.jpg is user 7.
  const fileRef = ref(storage, `profile_pictures/${uid}.jpg`);
  return withUploadIdentity(async () => {
    await uploadBytes(fileRef, blob);
    return await getDownloadURL(fileRef); // public URL
  });
}


export async function uploadVideo(uri) {
 
  const fileSize = await getFileSize(uri);
  let compressedUri = uri;

  if (fileSize > VIDEO_HARD_LIMIT) {
    throw new Error("Video too large (max 100MB). Please choose a smaller file.");
  }

  if (fileSize > VIDEO_SOFT_LIMIT) {
    console.log("Compressing video before upload...");
    // Reduce resolution + apply medium compression
    compressedUri = await Video.compress(
      uri,
      {
        compressionMethod: "auto",
        minimumFileSizeForCompress: 0,
        quality: "medium",
        width: 1280, // ~720p
      },
      (progress) => console.log(`Compressing: ${progress * 100}%`)
    );
  }
  
  const response = await fetch(compressedUri);
  const blob = await response.blob();

  const fileRef = ref(storage, `videos/${uuid.v4()}.mp4`);
  return withUploadIdentity(async () => {
    await uploadBytes(fileRef, blob);
    return await getDownloadURL(fileRef);
  });
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


