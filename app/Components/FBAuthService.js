import { initializeAuth, inMemoryPersistence, signInWithCustomToken, signOut } from "firebase/auth";
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

// In memory on purpose. A Firebase session here is derived from our own, lasts
// an hour, and costs one request to re-establish - so writing it to disk would
// persist a credential past the logout that should have ended it, to save a
// round trip per launch.
const auth = initializeAuth(firebaseApp, { persistence: inMemoryPersistence });

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
  if (auth.currentUser) {
    return auth.currentUser;
  }

  if (!signingIn) {
    signingIn = exchange().finally(() => {
      signingIn = null;
    });
  }

  return signingIn;
}

async function exchange() {
  const { data } = await axios.post(IP_STRING + "/api/media/token");
  const credential = await signInWithCustomToken(auth, data.token);
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
  if (auth.currentUser) {
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
  await ensureUploadIdentity();

  try {
    return await upload();
  } catch (error) {
    if (error?.code !== "storage/unauthorized" && error?.code !== "storage/unauthenticated") {
      throw error;
    }

    await clearUploadIdentity();
    await ensureUploadIdentity();
    return upload();
  }
}
