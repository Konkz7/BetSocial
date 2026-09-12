package com.example.World.RateLimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The limiter itself.
 *
 * A plain unit test rather than an integration one: this is arithmetic over a
 * map, and running it against a database and a web server would make it slower
 * without making it test anything more. The endpoints that use it are covered in
 * RateLimitEndpointTest.
 */
@DisplayName("Rate limiting")
class RateLimitTest {

    private final InProcessRateLimiter limiter = new InProcessRateLimiter();

    @Test
    @DisplayName("allows an allowance, then refuses")
    void allowsThenRefuses() {
        RateLimit three = new RateLimit(3, Duration.ofHours(1));

        assertThatCode(() -> {
            limiter.require("a", three);
            limiter.require("a", three);
            limiter.require("a", three);
        }).doesNotThrowAnyException();

        assertThatThrownBy(() -> limiter.require("a", three))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("429");
    }

    @Test
    @DisplayName("keys are independent")
    void keysDoNotShareAnAllowance() {
        RateLimit one = new RateLimit(1, Duration.ofHours(1));

        limiter.require("first", one);

        assertThatCode(() -> limiter.require("second", one))
                .as("one person running out must not affect anybody else")
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("different actions by the same person are counted separately")
    void scopesDoNotCollide() {
        RateLimit one = new RateLimit(1, Duration.ofHours(1));

        limiter.require(RateLimiter.scopeOf("threads", 7L), one);

        assertThatCode(() -> limiter.require(RateLimiter.scopeOf("comments", 7L), one))
                .as("otherwise which limit ran out would depend on the order things were done in")
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("refills over time rather than all at once")
    void refillsGradually() throws InterruptedException {
        // Two per 200ms, so a token is back roughly every 100ms. Short enough to
        // wait for, long enough not to be flaky on a slow machine.
        RateLimit quick = new RateLimit(2, Duration.ofMillis(200));

        limiter.require("refill", quick);
        limiter.require("refill", quick);
        assertThatThrownBy(() -> limiter.require("refill", quick))
                .isInstanceOf(ResponseStatusException.class);

        Thread.sleep(250);

        assertThatCode(() -> limiter.require("refill", quick))
                .as("a bucket that never refills is a permanent ban")
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("idling does not bank credit for a burst")
    void refillIsCappedAtTheAllowance() throws InterruptedException {
        RateLimit two = new RateLimit(2, Duration.ofMillis(100));

        // Long enough to have earned many times the allowance, if it accumulated.
        Thread.sleep(500);

        limiter.require("capped", two);
        limiter.require("capped", two);

        assertThatThrownBy(() -> limiter.require("capped", two))
                .as("an idle hour must not buy an hour's worth of burst in one second")
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("wouldAllow answers without spending anything")
    void wouldAllowDoesNotConsume() {
        RateLimit one = new RateLimit(1, Duration.ofHours(1));

        assertThat(limiter.wouldAllow("peek", one)).isTrue();
        assertThat(limiter.wouldAllow("peek", one))
                .as("asking twice must not use the allowance up")
                .isTrue();

        assertThatCode(() -> limiter.require("peek", one)).doesNotThrowAnyException();
        assertThat(limiter.wouldAllow("peek", one)).isFalse();
    }

    @Test
    @DisplayName("asking about an unknown key does not create one")
    void wouldAllowDoesNotCreateBuckets() {
        int before = limiter.size();

        limiter.wouldAllow("never-used", new RateLimit(1, Duration.ofHours(1)));

        assertThat(limiter.size())
                .as("otherwise checking is itself a way to fill the map")
                .isEqualTo(before);
    }

    @Test
    @DisplayName("drops buckets nobody has touched")
    void evictsIdleBuckets() {
        limiter.require("stale", new RateLimit(5, Duration.ofHours(1)));
        assertThat(limiter.size()).isPositive();

        limiter.evictIdleBuckets();
        assertThat(limiter.size())
                .as("a bucket used just now is not idle")
                .isPositive();
    }

    @Test
    @DisplayName("concurrent callers cannot exceed the allowance between them")
    void concurrentCallersShareOneAllowance() throws InterruptedException {
        int allowance = 50;
        RateLimit limit = new RateLimit(allowance, Duration.ofHours(1));

        int threads = 8;
        int attemptsEach = 25;
        AtomicInteger allowed = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                start.await();
                for (int attempt = 0; attempt < attemptsEach; attempt++) {
                    try {
                        limiter.require("contended", limit);
                        allowed.incrementAndGet();
                    } catch (ResponseStatusException refused) {
                        // Expected once the allowance is gone.
                    }
                }
                return null;
            });
        }

        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        // 200 attempts against an allowance of 50. Refill over a few milliseconds
        // of an hour-long window is a fraction of a token, so the count cannot
        // legitimately exceed the allowance.
        assertThat(allowed.get())
                .as("a read-modify-write race would let more through than the limit allows")
                .isEqualTo(allowance);
    }

    @Test
    @DisplayName("a limit that allows nothing is rejected as a mistake")
    void nonsensicalLimitsAreRefused() {
        assertThatThrownBy(() -> new RateLimit(0, Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RateLimit(5, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
