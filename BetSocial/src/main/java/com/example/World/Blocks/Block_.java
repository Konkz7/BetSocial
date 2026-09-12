package com.example.World.Blocks;

import org.springframework.data.annotation.Id;
import org.springframework.lang.NonNull;

/**
 * One person having blocked another.
 *
 * Stored one-way and read both ways - see BlockRepository.invisibleTo. There is
 * no deleted_at: unblocking removes the row, because a block that has been
 * lifted is not history anybody needs and keeping it would only complicate the
 * uniqueness rule.
 */
public record Block_(

        @Id
        Long blid,

        /** Who did the blocking. */
        @NonNull
        Long blocker_uid,

        /** Who was blocked. */
        @NonNull
        Long blocked_uid,

        @NonNull
        Long created_at
) {
}
