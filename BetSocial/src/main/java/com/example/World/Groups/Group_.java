package com.example.World.Groups;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.lang.NonNull;



/**
 * A conversation: a set of members with messages in it.
 *
 * There is no separate kind for a direct message. Whether a conversation is
 * direct is carried by {@link #group_name}: null means a private conversation
 * between exactly two people, which the client titles from the other member, and
 * a name means a group. The old {@code sort} column said the same thing a second
 * time and is no longer read or written.
 */
public record Group_(
        @Id
        Long gid, // Primary key

        /** The group's name, or null for a direct conversation between two people. */
        String group_name,

        Long last_mid,
        @NonNull
        Long created_at, // Timestamp of conversation creation
        Long deleted_at,
        @Version
        Integer g_version // Version number for optimistic locking
) {
}
