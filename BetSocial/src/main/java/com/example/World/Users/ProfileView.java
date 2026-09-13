package com.example.World.Users;

/**
 * The caller's own profile.
 *
 * A superset of {@link UserView} with the role added. Kept separate rather than
 * adding the role to UserView, because UserView is what every other user sees on
 * the feed, in comments and in a member list - and which accounts are privileged
 * is not something to hand out with each of those. Here it is the caller's own
 * role, which they can hardly be kept from knowing.
 *
 * The client needs it to decide which interface to open after signing in.
 */
public record ProfileView(
        Long uid,
        String user_name,

        /**
         * The caller's own address, and only ever their own - this record is
         * returned by one session-gated endpoint and nothing else.
         *
         * Settings draws the change-password prompt from it, masked, so somebody
         * can see which address a reset link is about to go to. That row read
         * the field before it existed and so was permanently stuck on "your
         * account details are still loading".
         */
        String email,
        String bio,
        String profile_picture,
        String status,
        Boolean is_verified,
        Long created_at,

        /** 0 ordinary, 1 superuser, 2 admin - see UserRole. */
        Integer user_role
) {
    public static ProfileView from(User_ user) {
        return new ProfileView(
                user.uid(),
                user.user_name(),
                user.email(),
                user.bio(),
                user.profile_picture(),
                user.status(),
                user.is_verified(),
                user.created_at(),
                user.user_role()
        );
    }
}
