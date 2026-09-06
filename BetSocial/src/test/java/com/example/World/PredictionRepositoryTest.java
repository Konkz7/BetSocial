package com.example.World;

import com.example.World.Bets.BetRepository;
import com.example.World.Bets.Bet_;
import com.example.World.Bets.Status;
import com.example.World.Predictions.PredictionRepository;
import com.example.World.Predictions.Prediction_;
import com.example.World.Threads.ThreadRepository;
import com.example.World.Threads.Thread_;
import com.example.World.Users.UserRepository;
import com.example.World.Users.User_;
import com.example.World.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for a data-corruption bug.
 *
 * PredictionRepository.remove was written as
 *     UPDATE Bet_ ... WHERE bid = :id
 * copy-pasted from BetRepository.remove. Removing a prediction therefore
 * soft-deleted the unrelated bet whose bid happened to equal the prediction's
 * pid, and left the prediction itself active.
 *
 * The collision is arranged explicitly rather than left to chance: a decoy bet
 * is inserted with bid == pid so the buggy query would always have a row to
 * destroy. Without that the test could pass vacuously.
 */
@DisplayName("PredictionRepository.remove")
class PredictionRepositoryTest extends AbstractIntegrationTest {

    private static final AtomicLong PHONE = new AtomicLong(2_340_000_000_000L);

    @Autowired UserRepository users;
    @Autowired ThreadRepository threads;
    @Autowired BetRepository bets;
    @Autowired PredictionRepository predictions;

    @Test
    @DisplayName("soft-deletes the prediction, not the bet whose bid matches its pid")
    void removesThePredictionNotTheBet() {
        long now = new Date().getTime();

        User_ author = users.save(user("phase4-author"));
        User_ punter = users.save(user("phase4-punter"));

        Thread_ thread = threads.save(new Thread_(
                null, author.uid(), "phase4 regression thread", null, 0,
                "test", 0L, now, null, false, null));

        Bet_ bet = bets.save(newBet(thread.tid(), now));

        Prediction_ prediction = predictions.save(new Prediction_(
                null, bet.bid(), punter.uid(), true, 10f, 0f, now, null, null));

        // Guarantee a bet exists at bid == pid - the row the old query would hit.
        long collidingBid = prediction.pid();
        if (jdbc.queryForObject("SELECT count(*) FROM bet_ WHERE bid = ?", Integer.class, collidingBid) == 0) {
            jdbc.update("""
                    INSERT INTO bet_ (bid, tid, status, amount_for, amount_against, description,
                                      created_at, ends_at, is_verified, king_mode, profit_mode,
                                      max_amount, min_amount, b_version)
                    VALUES (?, ?, ?, 0, 0, 'phase4 decoy bet', ?, ?, false, false, false, 100, 1, 0)
                    """, collidingBid, thread.tid(), Status.ACTIVE.toInt(), now, now + 86_400_000L);
        }

        // Delete strictly after creation. The entities override created_at() to throw
        // when created_at >= deleted_at, so a same-millisecond soft delete makes the
        // row permanently unserializable - see the note on this in the PR.
        long deletedAt = now + 1_000L;
        predictions.remove(prediction.pid(), deletedAt);

        assertThat(jdbc.queryForObject(
                "SELECT deleted_at FROM prediction_ WHERE pid = ?", Long.class, prediction.pid()))
                .as("the prediction itself should be soft-deleted")
                .isEqualTo(deletedAt);

        assertThat(jdbc.queryForObject(
                "SELECT deleted_at FROM bet_ WHERE bid = ?", Long.class, collidingBid))
                .as("the bet at bid == pid must survive - this is the corruption the fix prevents")
                .isNull();
    }

    private Bet_ newBet(Long tid, long now) {
        return new Bet_(null, tid, Status.ACTIVE.toInt(), null, 0f, 0f,
                "phase4 regression bet", now, null, now + 86_400_000L,
                false, false, false, 100f, 1f, null);
    }

    private User_ user(String name) {
        long now = new Date().getTime();
        String unique = name + "-" + PHONE.get();
        return new User_(
                null, unique, unique + "@example.test",
                "$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUV",
                "+" + PHONE.incrementAndGet(),
                null, true, "", null, now, null, 0, null, "offline", null, 0.0, null);
    }
}
