// Firebase client configuration.
//
// NOT the service-account JSON. The project has two sets of Firebase credentials
// and they are not interchangeable:
//
//   this file                                the WEB APP config. Public by
//                                            design - it is bundled into the app
//                                            and ships on every device.
//
//   BetSocial/src/main/resources/            the ADMIN SDK service account. A
//     firebaseAPI.json                       private key with full access to the
//                                            project. Server-side only, never
//                                            here.
//
// Putting the service account in this file bundles that private key into the
// app, and fails with storage/no-default-bucket anyway, because its keys are
// snake_case and its bucket carries a gs:// prefix.
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
