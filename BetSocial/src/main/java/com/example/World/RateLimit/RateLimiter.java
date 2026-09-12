package com.example.World.RateLimit;

/**
 * Refuses a caller who is going too fast.
 *
 * An interface with one method, and one implementation today, because the
 * implementation is the part expected to change. In-process counting is right
 * while there is one server and wrong the moment there are two - each would
 * enforce its own share of the limit - so the intent is that this is swapped for
 * something shared (Redis) at the point that happens, changing one class rather
 * than every caller.
 */
public interface RateLimiter {

    /**
     * Records one use against a key, or throws 429 if the allowance is spent.
     *
     * Throwing rather than returning a boolean on purpose: every caller would
     * otherwise write the same if-statement and the same response, and the one
     * that forgot would silently have no limit at all.
     *
     * @param key   who is being limited, and at what. Keys from different call
     *              sites must not collide, which is what scopeOf is for.
     */
    void require(String key, RateLimit limit);

    /**
     * Whether the next require would succeed, spending nothing.
     *
     * Exists for one case: sign-in, where the thing worth limiting is failed
     * attempts rather than attempts. Counting every attempt would lock out
     * somebody who simply signs in often, and counting failures alone cannot
     * refuse anything - by the time a failure is known, the password has already
     * been checked, which is the thing brute force wants. So the failure handler
     * spends a token and the filter in front asks this first.
     */
    boolean wouldAllow(String key, RateLimit limit);

    /**
     * Builds a key that cannot collide with another call site's.
     *
     * "user:4" is not enough - the same person posting and commenting would share
     * one allowance, and which of the two ran out would depend on the order they
     * did things in.
     */
    static String scopeOf(String action, Object identity) {
        return action + ':' + identity;
    }
}
