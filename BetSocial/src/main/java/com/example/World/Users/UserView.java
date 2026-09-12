package com.example.World.Users;

/**
 * Public projection of {@link User_} used for every API response.
 *
 * The entity was previously serialised directly, which exposed the BCrypt
 * pass_word hash, verification_token, fb_notification_token, phone_number,
 * email, wallet_address and balance to any authenticated caller - including
 * on the main feed, since ThreadProfile embedded a whole User_ per thread.
 *
 * Only fields the client actually renders are included:
 *   uid, user_name, profile_picture, bio  (profiles, feed, comments)
 *   status                                (DM online indicator)
 *   is_verified, created_at               (non-sensitive, kept for display)
 */
public record UserView(
        Long uid,
        String user_name,
        String bio,
        String profile_picture,
        String status,
        Boolean is_verified,
        Long created_at
) {
    /** What a deleted account is called wherever its content still appears. */
    public static final String TOMBSTONE_NAME = "Deleted user";

    public static UserView from(User_ user) {
        // A deleted account keeps its threads, comments and messages, so it still
        // has to render somewhere. The stored name is scrubbed to something
        // unique and unusable - user_name is a unique column, so every deleted
        // account cannot literally be called "Deleted user" - and the presentable
        // version is produced here rather than stored.
        //
        // Bio, picture and status go too. They are personal data, and a profile
        // picture surviving an account deletion is exactly the kind of thing the
        // right to erasure exists about.
        if (user.deleted_at() != null) {
            return new UserView(
                    user.uid(),
                    TOMBSTONE_NAME,
                    "",
                    null,
                    "offline",
                    false,
                    user.created_at()
            );
        }

        return new UserView(
                user.uid(),
                user.user_name(),
                user.bio(),
                user.profile_picture(),
                user.status(),
                user.is_verified(),
                user.created_at()
        );
    }
}
