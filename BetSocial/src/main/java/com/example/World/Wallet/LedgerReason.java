package com.example.World.Wallet;

/**
 * Why coins moved.
 *
 * Every entry carries one, so a balance can always be broken down into what
 * caused it rather than just totalled. The database has a matching check
 * constraint - adding a value here means adding it there.
 */
public enum LedgerReason {

    /** What a new account starts with. Once per user, ever. */
    OPENING_GRANT,

    /** The periodic top-up, so that running out costs a day rather than the account. */
    DAILY_TOPUP,

    /** Taken when a prediction is placed. Negative. */
    STAKE,

    /**
     * Given back: the prediction was withdrawn or changed, the bet was cancelled
     * or rejected, or it settled with nobody on the winning side.
     */
    STAKE_REFUND,

    /** Paid out on a bet that was won. */
    WINNINGS,

    /** A correction. Entries are never edited, so putting one right is a new row. */
    ADJUSTMENT
}
