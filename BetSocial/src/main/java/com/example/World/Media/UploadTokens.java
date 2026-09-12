package com.example.World.Media;

import java.time.Duration;

/**
 * Gives an upload an identity Firebase will recognise.
 *
 * Sign-in here is a session against our own database; Firebase has never been
 * told who anybody is. So every upload arrived at the bucket anonymous, and the
 * Storage rules could only ever constrain <em>what</em> was being written - a
 * size and a content type - never <em>who</em> was writing it. Anybody holding
 * the client config, which ships inside the app, could write to the bucket.
 *
 * A custom token closes that. The server signs a short assertion that "this is
 * user 7" with the service-account key it already holds, the client exchanges it
 * for a Firebase session, and the rules can then require an identity and pin a
 * profile picture to its owner.
 *
 * An interface for the same reason as {@link MediaStore}: it keeps the one call
 * that needs real credentials out of everything that wants to test the endpoint.
 */
public interface UploadTokens {

    /**
     * How long a minted token stays usable.
     *
     * Firebase fixes this at an hour and does not let it be configured, so this
     * is a statement of that rather than a choice. It only bounds the exchange -
     * once swapped for a Firebase session, the client refreshes on its own.
     */
    Duration LIFETIME = Duration.ofHours(1);

    /**
     * A token asserting that the bearer is this user.
     *
     * @param userId our own user id, which becomes the Firebase uid - so the
     *               rules can compare it against a path like
     *               {@code profile_pictures/7.jpg} without a second mapping to
     *               keep in step.
     */
    String forUser(long userId);
}
