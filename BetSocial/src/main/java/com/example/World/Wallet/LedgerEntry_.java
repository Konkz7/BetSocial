package com.example.World.Wallet;

import org.springframework.data.annotation.Id;
import org.springframework.lang.NonNull;

/**
 * One movement of coins.
 *
 * Append-only: there is no deleted_at and nothing updates a row once written. A
 * mistake is corrected by adding an entry that reverses it, which is what makes
 * the history worth reading.
 */
public record LedgerEntry_(

        @Id
        Long leid,

        @NonNull
        Long uid,

        /** Signed, in whole coins. Negative takes them away. */
        @NonNull
        Long amount,

        @NonNull
        String reason,

        /** The bet this relates to, when it relates to one. */
        Long bid,

        /**
         * What happened, in words, as it was at the time. Held here rather than
         * looked up on read so that renaming or removing a bet cannot rewrite what
         * the ledger says about a movement that already happened.
         */
        @NonNull
        String description,

        @NonNull
        Long created_at
) {
}
