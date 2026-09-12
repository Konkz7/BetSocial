package com.example.World.RateLimit;

import java.time.Duration;

/**
 * How much of something one caller may do, and over what period.
 *
 * A record rather than two loose numbers so a limit can be named once and
 * referred to everywhere - see Limits. Expressed as "allowance per window"
 * because that is how the decision is actually discussed ("ten threads an
 * hour"), and converted to a refill rate internally.
 */
public record RateLimit(int allowance, Duration window) {

    public RateLimit {
        if (allowance <= 0) {
            throw new IllegalArgumentException("A limit that allows nothing is a closed door");
        }
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("A limit needs a window to be measured over");
        }
    }

    /** Tokens gained per millisecond, which is how the bucket actually refills. */
    double refillPerMilli() {
        return (double) allowance / window.toMillis();
    }
}
