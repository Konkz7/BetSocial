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
    public static UserView from(User_ user) {
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
