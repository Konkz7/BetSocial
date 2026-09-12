import { initializeAuth, getAuth, inMemoryPersistence, signInWithCustomToken, signOut } from "firebase/auth";
import axios from "axios";
import { firebaseApp, IP_STRING } from "../Constants";

/**
 * Gives Storage uploads an identity.
 *
 * Signing in to this app is a session against our own database - Firebase was
 * never told who anybody was, so every upload reached the bucket anonymous. That
 * capped what the Storage rules could check at "is this a small enough image",
 * never "is this person allowed to write here", which left the bucket writable
 * by anyone holding the client config that ships inside the app.
 *
 * So: the server signs a short assertion that the session belongs to user 7, and
 * this exchanges it for a Firebase session. The rules can then require an
 * identity, and pin profile_pictures/7.jpg to the person who owns it.
 *
 * This is the JS SDK's auth, which is a separate instance from the
 * @react-native-firebase one used for phone OTP at registration. They do not
 * share a signed-in user, and this is the one Storage uploads go through.
 */

let auth = null;

/**
 * The auth instance, created on first use.
 *
 * Not at module scope. Firebase Auth validates the config as it initialises and
 * throws if the apiKey is missing, and a throw while a module is being evaluated
 * leaves every one of its exports undefined - so one bad config key turned into
 * "Cannot read property 'clearUploadIdentity' of undefined" at a call site that
 * has nothing to do with the cause. Doing it lazily keeps the failure where it
 * happened and keeps the module importable.
 *
 * In memory on purpose. A Firebase session here is derived from our own, lasts
 * an hour, and costs one request to re-establish - so writing it to disk would
 * persist a credential past the logout that should have ended it, to save a
 * round trip per launch.
 */
function uploadAuth() {
  if (auth) {
    return auth;
  }

  try {
    auth = initializeAuth(firebaseApp, { persistence: inMemoryPersistence });
  } catch (error) {
    if (error?.code === "auth/already-initialized") {
      auth = getAuth(firebaseApp);
    } else {
      throw error;
    }
  }

  return auth;
}

/** One in-flight exchange, shared. */
let signingIn = null;

/**
 * Signs in to Firebase if we are not already, and resolves when uploads can go.
 *
 * Concurrent callers share one exchange rather than racing: picking several
 * images at once would otherwise mint a token per upload and spend the rate
 * limit on a single post.
 */
export async function ensureUploadIdentity() {
  const instance = uploadAuth();

  if (instance.currentUser) {
    return instance.currentUser;
  }

  if (!signingIn) {
    signingIn = exchange(instance).finally(() => {
      signingIn = null;
    });
  }

  return signingIn;
}

async function exchange(instance) {
  const { data } = await axios.post(IP_STRING + "/api/media/token");
  const credential = await signInWithCustomToken(instance, data.token);
  return credential.user;
}

/**
 * Drops the Firebase identity. Call this when the person signs out of the app.
 *
 * Without it their Firebase session outlives their session with us, and the next
 * person to use the device uploads as them.
 */
export async function clearUploadIdentity() {
  signingIn = null;

  // Nothing to sign out of if auth never came up - which is the normal case on
  // a device that has not uploaded anything this run, and also the case when the
  // config is wrong. Logging out must work either way.
  if (auth?.currentUser) {
    await signOut(auth);
  }
}

/**
 * Runs an upload, signing in first and once more if the identity turned out to
 * be stale.
 *
 * The retry is for the case that cannot be checked in advance: a Firebase
 * session can be revoked or expire between the check and the upload, and the
 * only way to find out is to be refused. One retry, so a genuine permission
 * problem still surfaces as an error rather than a loop.
 */
export async function withUploadIdentity(upload) {
  let identified = false;

  try {
    await ensureUploadIdentity();
    identified = true;
  } catch (error) {
    // Attempt the upload anyway rather than refusing it here.
    //
    // The client is not what enforces any of this - the Storage rules are. If
    // they require an identity, the bucket refuses this in a moment and the
    // screen says so; if they do not, refusing here would break uploading for a
    // reason the rules do not actually care about. Either way the useful thing
    // is to say why the identity is missing, because "no default bucket" and
    // "invalid api key" both point at the same file.
    console.error(
      "Uploading without an identity - the Storage rules will refuse this if " +
      "they require one. Cause:", error?.code || error?.message || error,
      "\nThis usually means app/Secrets.js is not the web app config: Auth " +
      "needs apiKey, appId, authDomain and messagingSenderId, which a " +
      "service-account JSON does not have. See app/Secrets.example.js."
    );
  }

  try {
    return await upload();
  } catch (error) {
    const refused = error?.code === "storage/unauthorized"
      || error?.code === "storage/unauthenticated";

    // Only worth retrying if we had an identity that might have gone stale.
    // Without one, a refusal is the rules working as intended and a second
    // attempt would be refused identically.
    if (!refused || !identified) {
      throw error;
    }

    await clearUploadIdentity();
    await ensureUploadIdentity();
    return upload();
  }
}
