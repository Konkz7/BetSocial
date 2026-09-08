package com.example.World.Bets;

/**
 * A bet waiting to be approved, with everything needed to decide on it.
 *
 * Assembled server-side rather than left to the client to gather. An approver
 * would otherwise fetch the bet, then its thread for the title, then its
 * predictions to count them - three round trips per row, on a screen whose whole
 * point is getting through a list quickly.
 */
public record PendingBetView(

        Long bid,
        String description,

        /** The title of the thread it was posted under, for context. */
        String thread_title,

        /** What the bet's owner says happened. This is what is being approved. */
        Boolean outcome,

        long amount_for,
        long amount_against,

        /** How many people staked on it, and how much is riding on the decision. */
        int prediction_count,
        long total_staked,

        Long ends_at
) {
}
