package com.example.World;

import com.example.World.Bets.BetRepository;
import com.example.World.Bets.Bet_;
import com.example.World.Predictions.PredictionHistory;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The list behind the Predictions tab.
 *
 * The tab set a state nothing read - the threads list rendered underneath it
 * either way, so it did nothing at all. What it needs is not the predictions
 * themselves, which are a bid, a side and two numbers, but what each one was
 * about.
 *
 * amount_won is already the net result, and the four cases it encodes are the
 * substance of the screen: still running, lost, refunded, won. Nothing should be
 * doing arithmetic on it afterwards.
 */
@DisplayName("Prediction history")
class PredictionHistoryTest extends AbstractIntegrationTest {

    // See SecurityRegressionTest for the per-class phone_number blocks in use.
    private static final AtomicLong PHONE = new AtomicLong(2_372_000_000_000L);

    @Autowired PredictionRepository predictions;
    @Autowired BetRepository bets;
    @Autowired ThreadRepository threads;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("says what each wager was about")
    void carriesTheContext() {
        User_ punter = user();
        Thread_ thread = thread("Who wins the derby");
        Bet_ bet = bet(thread, "Arsenal to score first");
        stake(bet, punter, true, 100);

        PredictionHistory row = predictions.historyOf(punter.uid()).get(0);

        assertThat(row.thread_title())
                .as("a prediction on its own cannot say what was predicted, which is "
                        + "the one thing this list has to say")
                .isEqualTo("Who wins the derby");
        assertThat(row.bet_description()).isEqualTo("Arsenal to score first");
        assertThat(row.prediction()).isTrue();
        assertThat(row.amount_bet()).isEqualTo(100);
    }

    @Test
    @DisplayName("the four things amount_won means, unchanged")
    void reportsEveryOutcome() {
        User_ punter = user();
        Thread_ thread = thread("A thread");

        Prediction_ open = stake(bet(thread, "undecided"), punter, true, 10);
        Prediction_ lost = stake(bet(thread, "lost"), punter, false, 40);
        Prediction_ refunded = stake(bet(thread, "refunded"), punter, true, 25);
        Prediction_ won = stake(bet(thread, "won"), punter, true, 100);

        predictions.updateAmountWon(lost.pid(), -40L);
        predictions.updateAmountWon(refunded.pid(), 0L);
        predictions.updateAmountWon(won.pid(), 40L);

        List<PredictionHistory> history = predictions.historyOf(punter.uid());

        assertThat(amountWonFor(history, open.pid()))
                .as("still running, or closed and waiting on a decision")
                .isNull();
        assertThat(amountWonFor(history, lost.pid())).isEqualTo(-40L);
        assertThat(amountWonFor(history, refunded.pid()))
                .as("zero is a refund, not a loss - nobody backed the outcome and "
                        + "every stake went back")
                .isEqualTo(0L);
        assertThat(amountWonFor(history, won.pid()))
                .as("the profit, not the return: this stake was 100 and the wallet "
                        + "received 140")
                .isEqualTo(40L);
    }

    @Test
    @DisplayName("newest first")
    void ordersByWhenItWasStaked() {
        User_ punter = user();
        Thread_ thread = thread("A thread");

        Prediction_ older = stake(bet(thread, "older"), punter, true, 10);
        jdbc.update("UPDATE prediction_ SET created_at = ? WHERE pid = ?",
                new Date().getTime() - 86_400_000L, older.pid());
        Prediction_ newer = stake(bet(thread, "newer"), punter, true, 10);

        assertThat(predictions.historyOf(punter.uid()))
                .extracting(PredictionHistory::pid)
                .startsWith(newer.pid());
    }

    @Test
    @DisplayName("somebody else's wagers are not yours")
    void isPerPerson() {
        User_ punter = user();
        User_ stranger = user();
        Thread_ thread = thread("A thread");
        stake(bet(thread, "theirs"), stranger, true, 10);

        assertThat(predictions.historyOf(punter.uid())).isEmpty();
    }

    @Test
    @DisplayName("a withdrawn prediction drops out")
    void excludesWithdrawn() {
        User_ punter = user();
        Thread_ thread = thread("A thread");
        Prediction_ withdrawn = stake(bet(thread, "withdrawn"), punter, true, 10);

        predictions.remove(withdrawn.pid(), new Date().getTime());

        assertThat(predictions.historyOf(punter.uid())).isEmpty();
    }

    @Test
    @DisplayName("a deleted thread does not erase your wallet history")
    void keepsWagersOnRemovedThreads() {
        User_ punter = user();
        Thread_ thread = thread("A thread that gets deleted");
        stake(bet(thread, "still happened"), punter, true, 60);

        jdbc.update("UPDATE thread_ SET deleted_at = ? WHERE tid = ?",
                new Date().getTime(), thread.tid());

        assertThat(predictions.historyOf(punter.uid()))
                .as("the stake happened and the coins moved whether or not the "
                        + "thread still stands; a history with rows missing is worse "
                        + "than one naming something gone")
                .hasSize(1);
    }

    // --- helpers ----------------------------------------------------------

    private static Long amountWonFor(List<PredictionHistory> history, Long pid) {
        return history.stream().filter(h -> h.pid().equals(pid)).findFirst()
                .orElseThrow().amount_won();
    }

    private Prediction_ stake(Bet_ bet, User_ punter, boolean side, long amount) {
        return predictions.save(new Prediction_(null, bet.bid(), punter.uid(), side,
                amount, null, new Date().getTime(), null, null));
    }

    private Bet_ bet(Thread_ thread, String description) {
        return bets.save(new Bet_(null, thread.tid(), 0, null, 0L, 0L, description,
                new Date().getTime(), null, new Date().getTime() + 86_400_000L,
                 0L, 0L, null));
    }

    private Thread_ thread(String title) {
        return threads.save(new Thread_(null, user().uid(), title, null, 0,
                "test", 0L, new Date().getTime(), null, false, null));
    }

    private User_ user() {
        long seq = PHONE.incrementAndGet();
        String name = "history-" + seq;
        return users.save(new User_(null, name, name + "@example.test",
                passwordEncoder.encode("password"),
                "+" + seq, null, true, "", null, new Date().getTime(), null,
                0, null, "offline", null, 0.0, null));
    }
}
