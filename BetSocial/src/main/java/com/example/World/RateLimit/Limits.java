package com.example.World.RateLimit;

import java.time.Duration;

/**
 * Every limit in one place, so the numbers can be argued about without reading
 * the controllers.
 *
 * They are deliberately generous. The purpose is to stop a script, not to
 * inconvenience somebody having an enthusiastic evening - a limit low enough to
 * catch a real user is a limit that will be reported as a bug, and rightly.
 */
public final class Limits {

    /**
     * Registration, keyed by address rather than by account - there is no account
     * yet. Three is enough for somebody mistyping an email twice.
     */
    public static final RateLimit REGISTER = new RateLimit(5, Duration.ofHours(1));

    /**
     * Failed sign-ins, keyed by address. Guessing a password is the one case
     * where the limit is the security control rather than a courtesy, so this is
     * the tightest of them.
     */
    public static final RateLimit LOGIN = new RateLimit(10, Duration.ofMinutes(15));

    /** Posting. Ten an hour is far more than anybody writes and far less than a bot wants. */
    public static final RateLimit THREADS = new RateLimit(15, Duration.ofHours(1));

    public static final RateLimit COMMENTS = new RateLimit(60, Duration.ofHours(1));

    /**
     * Following. The abuse here is a script following thousands of accounts to
     * farm follow-backs, which needs a rate no person approaches.
     */
    public static final RateLimit FOLLOWS = new RateLimit(100, Duration.ofHours(1));

    /**
     * Messages. Per minute rather than per hour: a long conversation is normal
     * over an evening and abnormal over a minute.
     */
    public static final RateLimit MESSAGES = new RateLimit(60, Duration.ofMinutes(1));

    /**
     * Reports. Loose enough that somebody working through a genuinely bad account
     * is not stopped, tight enough that reports cannot be used to bury a
     * moderation queue.
     */
    public static final RateLimit REPORTS = new RateLimit(30, Duration.ofHours(1));

    /**
     * Asking for a reset link.
     *
     * The tightest of the lot after sign-in, because each one sends an email to
     * an address the caller chose. Unlimited, this endpoint is a way to send mail
     * to anybody, from us.
     */
    public static final RateLimit FORGOT_PASSWORD = new RateLimit(5, Duration.ofHours(1));

    /** Using a reset link. The token is unguessable; this only stops it being free to hammer. */
    public static final RateLimit RESET_PASSWORD = new RateLimit(20, Duration.ofHours(1));

    /** Placing a stake. Not abuse-prone - the wallet limits it - but a script should not be able to drain one. */
    public static final RateLimit PREDICTIONS = new RateLimit(60, Duration.ofHours(1));

    /**
     * Asking for an upload identity.
     *
     * A token lasts an hour and the client refreshes its Firebase session by
     * itself afterwards, so one launch needs one. Sixty covers a day of
     * restarts and still refuses a script collecting tokens to write to the
     * bucket with.
     */
    public static final RateLimit UPLOAD_TOKENS = new RateLimit(60, Duration.ofHours(1));

    private Limits() {
    }
}
