package com.example.World;

import com.example.World.Bets.BetRepository;
import com.example.World.Bets.Bet_;
import com.example.World.Predictions.BetSideCount;
import com.example.World.Predictions.PredictionRepository;
import com.example.World.Predictions.Prediction_;
import com.example.World.Predictions.ThreadStakeSummary;
import com.example.World.Threads.ThreadRepository;
import com.example.World.Threads.Thread_;
import com.example.World.Users.UserRepository;
import com.example.World.Users.User_;
import com.example.World.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The numbers the feed card and the thread page were inventing.
 *
 * The card said "$2.5K" and "18" on every thread, and the thread page said
 * "for / against" on every bet - the words rather than the amounts. These are
 * the queries behind the real ones.
 *
 * The counting is the part worth testing rather than the rendering: a bettor who
 * staked on two bets in one thread is one person, a soft-deleted prediction is
 * nobody, and a thread nobody has touched has to come back as zero rather than
 * as a missing row the card then renders as blank.
 */
@DisplayName("Stake summaries")
class StakeSummaryTest extends AbstractIntegrationTest {

    // See SecurityRegressionTest for the per-class phone_number blocks in use.
    private static final AtomicLong PHONE = new AtomicLong(2_371_000_000_000L);

    @Autowired PredictionRepository predictions;
    @Autowired BetRepository bets;
    @Autowired ThreadRepository threads;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("adds up what was staked, and counts the people once each")
    void countsPoolAndBettors() {
        Thread_ thread = thread();
        Bet_ first = bet(thread);
        Bet_ second = bet(thread);

        User_ punter = user();
        User_ other = user();

        stake(first, punter, true, 100);
        stake(second, punter, false, 50);   // same person, second bet
        stake(first, other, true, 75);

        ThreadStakeSummary summary = summaryOf(thread);

        assertThat(summary.pool()).isEqualTo(225);
        assertThat(summary.bettors())
                .as("somebody who staked on two bets in one thread is one person "
                        + "looking at that thread, not two")
                .isEqualTo(2);
    }

    @Test
    @DisplayName("a thread nobody has staked on is zero, not missing")
    void reportsNothingAsZero() {
        Thread_ untouched = thread();

        // Absent from the query result on purpose - manufacturing zero rows with
        // a LEFT JOIN across three tables costs more than defaulting. What matters
        // is that the caller does default, so the card shows 0 rather than blank.
        assertThat(predictions.stakesByThreadIds(List.of(untouched.tid()))).isEmpty();
    }

    @Test
    @DisplayName("a withdrawn prediction stops counting")
    void ignoresDeletedPredictions() {
        Thread_ thread = thread();
        Bet_ bet = bet(thread);
        Prediction_ withdrawn = stake(bet, user(), true, 500);
        stake(bet, user(), true, 20);

        predictions.remove(withdrawn.pid(), new Date().getTime());

        ThreadStakeSummary summary = summaryOf(thread);
        assertThat(summary.pool()).isEqualTo(20);
        assertThat(summary.bettors()).isEqualTo(1);
    }

    @Test
    @DisplayName("counts each side of each bet separately")
    void countsSides() {
        Thread_ thread = thread();
        Bet_ bet = bet(thread);
        Bet_ untouched = bet(thread);

        stake(bet, user(), true, 10);
        stake(bet, user(), true, 10);
        stake(bet, user(), false, 10);

        List<BetSideCount> counts = predictions.sideCountsByThread(thread.tid());

        BetSideCount found = counts.stream()
                .filter(c -> c.bid().equals(bet.bid())).findFirst().orElseThrow();

        assertThat(found.people_for()).isEqualTo(2);
        assertThat(found.people_against()).isEqualTo(1);

        assertThat(counts.stream().map(BetSideCount::bid))
                .as("a bet nobody has staked on has no row; the screen defaults to 0")
                .doesNotContain(untouched.bid());
    }

    @Test
    @DisplayName("one query covers a whole page of threads")
    void summarisesAPageAtOnce() {
        List<Thread_> page = List.of(thread(), thread(), thread());
        page.forEach(t -> stake(bet(t), user(), true, 30));

        List<ThreadStakeSummary> summaries = predictions.stakesByThreadIds(
                page.stream().map(Thread_::tid).toList());

        assertThat(summaries)
                .as("one lookup per card would be an N+1 growing with the page, "
                        + "which is what FeedQueryCountTest exists to catch")
                .hasSize(3);
        assertThat(summaries).allSatisfy(s -> assertThat(s.pool()).isEqualTo(30));
    }

    // --- helpers ----------------------------------------------------------

    private ThreadStakeSummary summaryOf(Thread_ thread) {
        return predictions.stakesByThreadIds(List.of(thread.tid()))
                .stream().findFirst().orElseThrow();
    }

    private Prediction_ stake(Bet_ bet, User_ punter, boolean side, long amount) {
        return predictions.save(new Prediction_(null, bet.bid(), punter.uid(), side,
                amount, null, new Date().getTime(), null, null));
    }

    private Bet_ bet(Thread_ thread) {
        return bets.save(new Bet_(null, thread.tid(), 0, null, 0L, 0L, "a bet",
                new Date().getTime(), null, new Date().getTime() + 86_400_000L,
                true, false, false, 0L, 0L, null));
    }

    private Thread_ thread() {
        return threads.save(new Thread_(null, user().uid(), "a thread", null, 0,
                "test", 0L, new Date().getTime(), null, false, null));
    }

    private User_ user() {
        long seq = PHONE.incrementAndGet();
        String name = "stakes-" + seq;
        return users.save(new User_(null, name, name + "@example.test",
                passwordEncoder.encode("password"),
                "+" + seq, null, true, "", null, new Date().getTime(), null,
                0, null, "offline", null, 0.0, null));
    }
}
