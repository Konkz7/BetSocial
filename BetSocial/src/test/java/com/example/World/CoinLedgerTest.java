package com.example.World;

import com.example.World.Users.UserRepository;
import com.example.World.Users.User_;
import com.example.World.Wallet.LedgerEntry_;
import com.example.World.Wallet.LedgerReason;
import com.example.World.Wallet.LedgerService;
import com.example.World.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A balance is the sum of the ledger rather than a column somebody edits.
 *
 * The arrangement this replaces had user_.balance as a mutable float that nothing
 * ever wrote, while settlement recorded payouts on prediction_.amount_won and
 * never touched it - so no figure could be explained by pointing at what caused
 * it, and the two never had to agree because neither was read.
 */
@DisplayName("Coin ledger")
class CoinLedgerTest extends AbstractIntegrationTest {

    // See SecurityRegressionTest for the per-class phone_number blocks in use.
    private static final AtomicLong PHONE = new AtomicLong(2_352_000_000_000L);

    private static final long ONE_DAY = 24L * 60 * 60 * 1000;

    @Autowired LedgerService ledger;
    @Autowired UserRepository users;

    @Test
    @DisplayName("a balance is what its entries add up to")
    void balanceIsTheSumOfEntries() {
        User_ user = users.save(user());
        ledger.grantOpeningBalance(user.uid());

        assertThat(ledger.balanceOf(user.uid())).isEqualTo(1_000);

        ledger.record(user.uid(), -250, LedgerReason.STAKE, 1L, "Stake on a bet");
        ledger.record(user.uid(), 400, LedgerReason.WINNINGS, 1L, "Won a bet");

        assertThat(ledger.balanceOf(user.uid()))
                .as("nothing set this number; it is the entries added up")
                .isEqualTo(1_150);
    }

    @Test
    @DisplayName("starts at nothing before anything has moved")
    void unknownBalanceIsZeroNotNull() {
        User_ user = users.save(user());

        // SUM over no rows is NULL in SQL, and a null balance would break every
        // caller that does arithmetic on it.
        assertThat(ledger.balanceOf(user.uid())).isZero();
    }

    @Test
    @DisplayName("refuses a spend the balance cannot cover, and says by how much")
    void spendingBeyondTheBalanceIsRefused() {
        User_ user = users.save(user());
        ledger.grantOpeningBalance(user.uid());

        assertThatThrownBy(() -> ledger.requireBalance(user.uid(), 1_001))
                .hasMessageContaining("1001")
                .hasMessageContaining("1000");

        // Exactly the balance is affordable; it is short of it that is not.
        ledger.requireBalance(user.uid(), 1_000);
    }

    @Test
    @DisplayName("a movement of nothing is not a movement")
    void zeroAmountIsRejected() {
        User_ user = users.save(user());

        assertThatThrownBy(() ->
                ledger.record(user.uid(), 0, LedgerReason.ADJUSTMENT, null, "Nothing happened"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("keeps what happened, in the order it happened")
    void historyIsNewestFirst() {
        User_ user = users.save(user());
        ledger.grantOpeningBalance(user.uid());
        ledger.record(user.uid(), -100, LedgerReason.STAKE, 7L, "Stake on bet 7");
        ledger.record(user.uid(), 180, LedgerReason.WINNINGS, 7L, "Won bet 7");

        List<LedgerEntry_> history = ledger.historyOf(user.uid());

        assertThat(history).extracting(LedgerEntry_::description)
                .containsExactly("Won bet 7", "Stake on bet 7", "Opening grant");

        // Each row says what caused it, so a balance can be broken down rather
        // than only totalled.
        assertThat(history).extracting(LedgerEntry_::reason)
                .containsExactly("WINNINGS", "STAKE", "OPENING_GRANT");
        assertThat(history.get(0).bid()).isEqualTo(7L);
    }

    @Test
    @DisplayName("the daily top-up arrives once a day, not once a visit")
    void topUpIsOncePerDay() {
        User_ user = users.save(user());
        ledger.grantOpeningBalance(user.uid());

        // The opening grant covers the first day, so nothing is due yet however
        // many times the wallet is opened.
        ledger.topUpIfDue(user.uid());
        ledger.topUpIfDue(user.uid());
        assertThat(ledger.balanceOf(user.uid()))
                .as("the grant is today's coins; a top-up on top would be two")
                .isEqualTo(1_000);

        backdateEverything(user.uid(), ONE_DAY + 1_000);

        ledger.topUpIfDue(user.uid());
        assertThat(ledger.balanceOf(user.uid())).isEqualTo(1_100);

        // Looking again the same day pays nothing more. This is what makes it safe
        // for a GET to collect it.
        ledger.topUpIfDue(user.uid());
        ledger.topUpIfDue(user.uid());
        assertThat(ledger.balanceOf(user.uid())).isEqualTo(1_100);
    }

    @Test
    @DisplayName("catches up whoever has been away, without a scheduled job")
    void topUpDoesNotDependOnTheServerHavingBeenUp() {
        User_ user = users.save(user());
        ledger.grantOpeningBalance(user.uid());

        // A week has passed and nothing was running to notice. A scheduled job
        // would have granted nothing for those days; this pays on the next look.
        backdateEverything(user.uid(), 7 * ONE_DAY);

        ledger.topUpIfDue(user.uid());

        assertThat(ledger.balanceOf(user.uid()))
                .as("one top-up on returning, not seven - the point is to unstick "
                    + "somebody who is broke, not to reward being away")
                .isEqualTo(1_100);
    }

    /** Moves a user's whole history back in time, to stand in for days passing. */
    private void backdateEverything(Long uid, long millis) {
        jdbc.update("UPDATE ledger_entry_ SET created_at = created_at - ? WHERE uid = ?",
                millis, uid);
    }

    private User_ user() {
        long seq = PHONE.incrementAndGet();
        String name = "ledger-" + seq;
        return new User_(null, name, name + "@example.test",
                "$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUV",
                "+" + seq, null, true, "", null, new Date().getTime(), null,
                0, null, "offline", null, 0.0, null);
    }
}
