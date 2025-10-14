import { getStorage, ref, uploadBytes, getDownloadURL } from "firebase/storage";
import { initializeApp } from "firebase/app";
import uuid from 'react-native-uuid';
import { launchImageLibrary } from 'react-native-image-picker';
import {firebaseApp} from "../Constants";
import { Image, Video } from 'react-native-compressor';
import RNFS from "react-native-fs";

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
  await uploadBytes(fileRef, blob);
  return await getDownloadURL(fileRef); // public URL
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

  const fileRef = ref(storage, `profile_pictures/${uid}.jpg`);
  await uploadBytes(fileRef, blob);
  return await getDownloadURL(fileRef); // public URL
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


