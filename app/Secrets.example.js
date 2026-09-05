// Firebase client configuration.
//
// Copy this file to Secrets.js and fill in the values from the Firebase console
// (Project settings -> General -> Your apps -> SDK setup and configuration):
//
//   cp app/Secrets.example.js app/Secrets.js
//
// Secrets.js is gitignored. This template is committed so a fresh clone knows
// the required shape - Constants.js imports { firebaseConfig } from "./Secrets",
// so the export name below must be kept exactly as-is.

export const firebaseConfig = {
  apiKey: "YOUR_API_KEY",
  authDomain: "YOUR_PROJECT.firebaseapp.com",
  databaseURL: "https://YOUR_PROJECT-default-rtdb.europe-west1.firebasedatabase.app/",
  projectId: "YOUR_PROJECT",
  storageBucket: "YOUR_PROJECT.firebasestorage.app",
  messagingSenderId: "YOUR_SENDER_ID",
  appId: "YOUR_APP_ID",
};
