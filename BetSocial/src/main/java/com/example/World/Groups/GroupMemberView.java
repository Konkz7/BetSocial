package com.example.World.Groups;

/**
 * A member of a conversation, as the client needs to draw them.
 *
 * The membership row alone carries only a uid, which is enough to decide who may
 * do what but not enough to put anybody on screen. Both screens that show members
 * need the same three things: a name to label them, a picture, and whether they
 * administer the group.
 *
 * Deliberately not the whole User_ - that record carries a password hash, a
 * verification token and a push token, none of which belong in a response.
 */
public record GroupMemberView(

        Long uid,
        String user_name,
        String profile_picture,
        boolean administrator
) {
}
