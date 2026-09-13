package com.example.World;

import com.example.World.Bets.BetRepository;
import com.example.World.Bets.BetSaveRepository;
import com.example.World.Bets.Bet_;
import com.example.World.Bets.Betsave_;
import com.example.World.Bets.SavedBetView;
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
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The list behind the Saved Bets row.
 *
 * Bookmarking has worked for as long as the thread screen has had the icon -
 * there is a table, a toggle and a per-bet lookup. What never existed was a way
 * to ask for the list, so the Settings row meant to show them did nothing.
 *
 * The interesting part is what a bookmark is for. It points at something to come
 * back to, so one pointing at a removed thread is worth dropping - which is the
 * opposite of a stake, where the record of coins moving stays true whatever
 * happened to the thread afterwards.
 */
@DisplayName("Saved bets")
class SavedBetsTest extends AbstractIntegrationTest {

    // See SecurityRegressionTest for the per-class phone_number blocks in use.
    private static final AtomicLong PHONE = new AtomicLong(2_374_000_000_000L);

    @Autowired BetSaveRepository saves;
    @Autowired BetRepository bets;
    @Autowired ThreadRepository threads;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("says what was saved, not just that something was")
    void carriesTheContext() {
        User_ punter = user();
        Thread_ thread = thread("Who wins the derby");
        Bet_ bet = bet(thread, "Arsenal to score first");
        save(bet, punter);

        SavedBetView row = saves.savedBy(punter.uid()).get(0);

        assertThat(row.thread_title()).isEqualTo("Who wins the derby");
        assertThat(row.bet_description())
                .as("a Betsave_ is two ids; on its own it cannot say what was "
                        + "saved, which is the only thing this list is for")
                .isEqualTo("Arsenal to score first");
        assertThat(row.bid()).isEqualTo(bet.bid());
    }

    @Test
    @DisplayName("somebody else's bookmarks are not yours")
    void isPerPerson() {
        User_ punter = user();
        User_ stranger = user();
        save(bet(thread("A thread"), "theirs"), stranger);

        assertThat(saves.savedBy(punter.uid())).isEmpty();
    }

    @Test
    @DisplayName("a bookmark on a removed thread drops out")
    void dropsPointersToNothing() {
        User_ punter = user();
        Thread_ thread = thread("A thread that gets deleted");
        save(bet(thread, "a bet"), punter);

        jdbc.update("UPDATE thread_ SET deleted_at = ? WHERE tid = ?",
                new Date().getTime(), thread.tid());

        assertThat(saves.savedBy(punter.uid()))
                .as("a bookmark is a pointer to something to come back to, and one "
                        + "pointing at something removed is not worth keeping - "
                        + "unlike a stake, which records coins that moved")
                .isEmpty();
    }

    @Test
    @DisplayName("a removed bet drops out too")
    void dropsRemovedBets() {
        User_ punter = user();
        Thread_ thread = thread("A thread");
        Bet_ bet = bet(thread, "a bet that gets removed");
        save(bet, punter);

        jdbc.update("UPDATE bet_ SET deleted_at = ? WHERE bid = ?",
                new Date().getTime(), bet.bid());

        assertThat(saves.savedBy(punter.uid())).isEmpty();
    }

    @Test
    @DisplayName("carries what the screen needs to say how it went")
    void carriesTheOutcome() {
        User_ punter = user();
        Bet_ bet = bet(thread("A thread"), "a bet");
        save(bet, punter);

        SavedBetView open = saves.savedBy(punter.uid()).get(0);
        assertThat(open.outcome())
                .as("null until somebody declares it, whatever the status says - "
                        + "the screen reads this rather than guessing from ends_at")
                .isNull();

        jdbc.update("UPDATE bet_ SET outcome = true WHERE bid = ?", bet.bid());

        assertThat(saves.savedBy(punter.uid()).get(0).outcome()).isTrue();
    }

    // --- helpers ----------------------------------------------------------

    private Betsave_ save(Bet_ bet, User_ punter) {
        return saves.save(new Betsave_(null, bet.bid(), punter.uid()));
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
        String name = "saved-" + seq;
        return users.save(new User_(null, name, name + "@example.test",
                passwordEncoder.encode("password"),
                "+" + seq, null, true, "", null, new Date().getTime(), null,
                0, null, "offline", null, 0.0, null));
    }
}
