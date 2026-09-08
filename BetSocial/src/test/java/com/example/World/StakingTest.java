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
import com.example.World.Wallet.LedgerService;
import com.example.World.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Staking, and what happens to a stake afterwards.
 *
 * Nothing was ever deducted for a prediction and nothing was ever credited for
 * winning one: settlement wrote a figure to prediction_.amount_won and no code
 * anywhere touched a balance. The pools were make-believe, and a user could stake
 * far more than they held because there was nothing to hold.
 */
@DisplayName("Staking")
class StakingTest extends AbstractIntegrationTest {

    // See SecurityRegressionTest for the per-class phone_number blocks in use.
    private static final AtomicLong PHONE = new AtomicLong(2_353_000_000_000L);

    @Autowired UserRepository users;
    @Autowired ThreadRepository threads;
    @Autowired BetRepository bets;
    @Autowired PredictionRepository predictions;
    @Autowired LedgerService ledger;
    @Autowired PasswordEncoder passwordEncoder;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("takes the stake when the prediction is placed")
    void stakeLeavesTheWalletImmediately() {
        User_ owner = user();
        User_ punter = user();
        Bet_ bet = activeBet(owner);

        assertThat(place(punter, bet, true, 250).getStatusCode()).isEqualTo(HttpStatus.CREATED);

        assertThat(ledger.balanceOf(punter.uid()))
                .as("coins leave now, not at settlement - otherwise the pools are "
                    + "promises rather than money")
                .isEqualTo(750);

        assertThat(bets.findById(bet.bid()).orElseThrow().amount_for())
                .as("and the pool holds what was actually staked")
                .isEqualTo(250);
    }

    @Test
    @DisplayName("refuses a stake the balance cannot cover")
    void cannotStakeMoreThanYouHold() {
        User_ owner = user();
        User_ punter = user();
        Bet_ bet = activeBet(owner);

        ResponseEntity<String> response = place(punter, bet, true, 1_001);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("1001").contains("1000");
        assertThat(ledger.balanceOf(punter.uid())).isEqualTo(1_000);
    }

    @Test
    @DisplayName("holds you to the bet's own limits")
    void limitsAreEnforcedOnTheServer() {
        User_ owner = user();
        User_ punter = user();
        // Between 10 and 100 coins.
        Bet_ bet = bets.save(newBet(owner, 100L, 10L));

        // These were checked in the client and nowhere else, so a request that did
        // not come from the client ignored them entirely.
        assertThat(place(punter, bet, true, 5).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(place(punter, bet, true, 500).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(place(punter, bet, true, 0).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(ledger.balanceOf(punter.uid())).isEqualTo(1_000);
        assertThat(place(punter, bet, true, 50).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @DisplayName("a stake cannot be changed or withdrawn once placed")
    void predictionsAreCommitted() {
        User_ owner = user();
        User_ punter = user();
        Bet_ bet = activeBet(owner);

        place(punter, bet, true, 100);

        // Switching sides after watching the pool build was free before, which made
        // deciding late strictly better than deciding well.
        assertThat(place(punter, bet, false, 100).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        Prediction_ placed = predictions.findByUidAndBid(punter.uid(), bet.bid()).orElseThrow();
        assertThat(exchange(HttpMethod.PUT, "/api/predictions/remove/" + placed.pid(), loginAs(punter))
                .getStatusCode())
                .as("and pulling out when it turns against you was free too")
                .isEqualTo(HttpStatus.CONFLICT);

        assertThat(ledger.balanceOf(punter.uid())).isEqualTo(900);
    }

    @Test
    @DisplayName("pays the winners their stake back plus a share of the other side")
    void winnersArePaidFromTheLosingPool() {
        User_ owner = user();
        User_ winnerA = user();
        User_ winnerB = user();
        User_ loser = user();

        Bet_ bet = activeBet(owner);
        place(winnerA, bet, true, 100);
        place(winnerB, bet, true, 300);
        place(loser, bet, false, 1_000);

        settle(owner, bet, true);

        // 400 backed the outcome, 1000 opposed it, and 80% of that is shared out.
        // A had a quarter of the winning side: 100/400 * 1000 * 0.8 = 200.
        assertThat(ledger.balanceOf(winnerA.uid()))
                .as("stake back plus a quarter of the paid-out pool")
                .isEqualTo(1_000 - 100 + 100 + 200);
        assertThat(ledger.balanceOf(winnerB.uid()))
                .as("three quarters of it")
                .isEqualTo(1_000 - 300 + 300 + 600);

        assertThat(ledger.balanceOf(loser.uid()))
                .as("the loser's stake went when they placed it; nothing more is taken")
                .isEqualTo(0);

        // 200 of the 1000 staked against is paid to nobody. With no house to take
        // it, it stops existing - which is what stops daily top-ups inflating
        // every balance for ever.
        assertThat(ledger.balanceOf(winnerA.uid()) + ledger.balanceOf(winnerB.uid())
                        + ledger.balanceOf(loser.uid()))
                .isEqualTo(3_000 - 200);
    }

    @Test
    @DisplayName("refunds everybody when nobody backed the outcome")
    void nobodyWinsMeansNobodyLoses() {
        User_ owner = user();
        User_ one = user();
        User_ two = user();

        Bet_ bet = activeBet(owner);
        place(one, bet, false, 200);
        place(two, bet, false, 300);

        // Everyone said no and the answer was yes. There is nobody to pay, and the
        // old arithmetic divided by an empty winning pool and stored an infinity.
        settle(owner, bet, true);

        assertThat(ledger.balanceOf(one.uid())).isEqualTo(1_000);
        assertThat(ledger.balanceOf(two.uid())).isEqualTo(1_000);
    }

    @Test
    @DisplayName("refunds everybody when the outcome is rejected")
    void rejectionRefunds() {
        User_ owner = user();
        User_ punter = user();

        Bet_ bet = activeBet(owner);
        place(punter, bet, true, 400);
        assertThat(ledger.balanceOf(punter.uid())).isEqualTo(600);

        decideOutcome(owner, bet, true);
        approve(bet, false);

        assertThat(ledger.balanceOf(punter.uid()))
                .as("nothing was decided, so nobody should be out of pocket for taking part")
                .isEqualTo(1_000);
    }

    // --- helpers ----------------------------------------------------------

    private ResponseEntity<String> place(User_ punter, Bet_ bet, boolean prediction, long amount) {
        return json("/api/predictions/make", HttpMethod.POST, loginAs(punter),
                Map.of("bid", bet.bid(), "prediction", prediction, "amount_bet", amount));
    }

    /** Runs a bet all the way to paid: closed, decided by its owner, approved by an admin. */
    private void settle(User_ owner, Bet_ bet, boolean outcome) {
        decideOutcome(owner, bet, outcome);
        approve(bet, true);
    }

    private void decideOutcome(User_ owner, Bet_ bet, boolean outcome) {
        // The scheduled sweep would do this a minute after ends_at; tests should not
        // wait for it.
        bets.updateStatus(bet.bid(), Status.PENDING.toInt());

        assertThat(json("/api/bets/decide", HttpMethod.POST, loginAs(owner),
                Map.of("bid", bet.bid(), "decision", outcome, "reason", "because"))
                .getStatusCode().is2xxSuccessful()).isTrue();
    }

    private void approve(Bet_ bet, boolean accept) {
        assertThat(json("/superusers/approval", HttpMethod.POST, adminSession(),
                Map.of("bid", bet.bid(), "decision", accept, "reason", "checked"))
                .getStatusCode().is2xxSuccessful()).isTrue();
    }

    private Bet_ activeBet(User_ owner) {
        return bets.save(newBet(owner, 0L, 0L));
    }

    private Bet_ newBet(User_ owner, long max, long min) {
        long now = new Date().getTime();
        Thread_ thread = threads.save(new Thread_(null, owner.uid(), "a thread", null, 0,
                "test", 0L, now, null, false, null));
        return bets.save(new Bet_(null, thread.tid(), Status.ACTIVE.toInt(), null, 0L, 0L,
                "will it happen", now, null, now + 86_400_000L,
                false, false, false, max, min, null));
    }

    private String adminSession() {
        return login("admin", "password");
    }

    private User_ user() {
        long seq = PHONE.incrementAndGet();
        String name = "staking-" + seq;
        User_ saved = users.save(new User_(null, name, name + "@example.test",
                passwordEncoder.encode("password"),
                "+" + seq, null, true, "", null, new Date().getTime(), null,
                0, null, "offline", null, 0.0, null));
        ledger.grantOpeningBalance(saved.uid());
        return saved;
    }

    private String loginAs(User_ user) {
        return login(user.user_name(), "password");
    }

    private String login(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", username);
        form.add("password", password);

        ResponseEntity<String> response =
                rest.postForEntity("/login", new HttpEntity<>(form, headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";")[0];
    }

    private ResponseEntity<String> json(String path, HttpMethod method, String session, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, session);
        try {
            return rest.exchange(path, method,
                    new HttpEntity<>(mapper.writeValueAsString(body), headers), String.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private ResponseEntity<String> exchange(HttpMethod method, String path, String session) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, session);
        return rest.exchange(path, method, new HttpEntity<>(headers), String.class);
    }
}
