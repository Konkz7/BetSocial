package com.example.World.Groups;

import org.springframework.data.annotation.Id;
import org.springframework.lang.NonNull;

public record Groupuser_(

    @Id
    Long guid,
    @NonNull
    Long gid,
    @NonNull
    Long uid,
    Long other_uid,
    @NonNull
    Long created_at,
    @NonNull
    Long last_read_timestamp,
    @NonNull
    Boolean administrator,

    /**
     * When this membership ended, or null while it is active.
     *
     * Membership is soft-deleted so that leaving or being removed stays visible
     * afterwards - a hard delete cannot be told apart from never having joined.
     * The group itself is the opposite: only ever hard-deleted, which cascades
     * these rows away with it.
     */
    Long deleted_at


) {
}
