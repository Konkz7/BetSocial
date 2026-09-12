package com.example.World.RateLimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token buckets held in memory.
 *
 * A bucket per key, refilling steadily up to its allowance. Chosen over a fixed
 * window counter because a window resets all at once: somebody who spends their
 * whole hour's allowance in the last second of one window can spend it again in
 * the first second of the next, which is twice the limit at exactly the moment
 * abuse looks like a burst. A bucket refills gradually, so the average holds.
 *
 * What this is not:
 *
 *   - Shared. Two instances of the application would each allow the full limit.
 *     That is fine today and wrong the moment anything is deployed twice, which
 *     is why callers depend on RateLimiter rather than on this.
 *   - Durable. A restart forgives everybody. For a courtesy limit that is
 *     acceptable; for the login limit it means a restart is a free set of
 *     guesses, which is worth remembering if restarts ever become frequent.
 *
 * Both are recorded here rather than in a ticket because the person who needs to
 * know is whoever is reading this class when they add a second server.
 */
@Component
public class InProcessRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(InProcessRateLimiter.class);

    /**
     * How long an untouched bucket is kept.
     *
     * Without this the map is a slow memory leak: one entry per address that ever
     * tried to register, kept for the life of the process. A full bucket is
     * indistinguishable from no bucket, so dropping an idle one loses nothing.
     */
    private static final Duration IDLE_BEFORE_EVICTION = Duration.ofHours(2);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public void require(String key, RateLimit limit) {
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> new Bucket(limit.allowance()));

        if (!bucket.tryConsume(limit, System.currentTimeMillis())) {
            // No detail about how long to wait: a caller who is being limited for
            // the right reasons is a script, and telling it exactly when to retry
            // is a favour to it. A person hits this so rarely that "slow down" is
            // the whole of the useful information.
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "That is happening too quickly. Try again shortly.");
        }
    }

    @Override
    public boolean wouldAllow(String key, RateLimit limit) {
        Bucket bucket = buckets.get(key);
        // No bucket means nothing has been spent, so the answer is yes - and
        // deliberately without creating one, or asking the question would itself
        // fill the map with entries for keys that never did anything.
        return bucket == null || bucket.hasToken(limit, System.currentTimeMillis());
    }

    /**
     * Drops buckets nobody has touched recently.
     *
     * Hourly, because the cost of holding a few thousand idle entries for an
     * extra hour is nothing and the cost of scanning the map often is not.
     */
    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void evictIdleBuckets() {
        long cutoff = System.currentTimeMillis() - IDLE_BEFORE_EVICTION.toMillis();
        int before = buckets.size();

        buckets.entrySet().removeIf(entry -> entry.getValue().lastUsedBefore(cutoff));

        int removed = before - buckets.size();
        if (removed > 0) {
            log.debug("Rate limiter: dropped {} idle buckets, {} remain", removed, buckets.size());
        }
    }

    /** Visible for testing: how many keys are currently held. */
    int size() {
        return buckets.size();
    }

    /**
     * One caller's allowance.
     *
     * Synchronised rather than built from atomics: consuming a token is a
     * read-modify-write over two fields that have to agree, and a lock over a few
     * arithmetic operations is both correct and cheaper to read than the
     * compare-and-set loop it would replace.
     */
    private static final class Bucket {

        private double tokens;
        private long lastRefill;

        private Bucket(int allowance) {
            this.tokens = allowance;
            this.lastRefill = System.currentTimeMillis();
        }

        private synchronized boolean tryConsume(RateLimit limit, long now) {
            refill(limit, now);

            if (tokens < 1.0) {
                return false;
            }
            tokens -= 1.0;
            return true;
        }

        /**
         * Whether a token is available, without taking one.
         *
         * Refills as a side effect, which is the same thing tryConsume does and
         * is not observable: refilling only ever moves time forward.
         */
        private synchronized boolean hasToken(RateLimit limit, long now) {
            refill(limit, now);
            return tokens >= 1.0;
        }

        private void refill(RateLimit limit, long now) {
            long elapsed = now - lastRefill;
            if (elapsed <= 0) {
                return;
            }

            // Capped at the allowance, so idling does not bank credit for a burst
            // far larger than the limit was ever meant to permit.
            tokens = Math.min(limit.allowance(), tokens + elapsed * limit.refillPerMilli());
            lastRefill = now;
        }

        private synchronized boolean lastUsedBefore(long cutoff) {
            return lastRefill < cutoff;
        }
    }
}
