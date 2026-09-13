package com.example.World;

import com.example.World.Bets.BetRepository;
import com.example.World.Bets.Bet_;
import com.example.World.Predictions.PredictionRecord;
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
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What another person's profile is allowed to say about your betting.
 *
 * The Predictions tab was dead on both profile screens. Finishing it for your
 * own was straightforward; finishing it for somebody else's is a decision, and
 * the decision was counts only.
 *
 * The reason is in the other query: historyOf joins Thread_ with no visibility
 * rules, deliberately, so your own history survives a thread being deleted.
 * Pointed at another person that would name private threads the viewer is not
 * allowed to know exist. A count cannot.
 */
@DisplayName("Prediction record")
class PredictionRecordTest extends AbstractIntegrationTest {

    // See SecurityRegressionTest for the per-class phone_number blocks in use.
    private static final AtomicLong PHONE = new AtomicLong(2_373_000_000_000L);

    @Autowired PredictionRepository predictions;
    @Autowired BetRepository bets;
    @Autowired ThreadRepository threads;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("counts what resolved, and what was right")
    void countsTheRecord() {
        User_ punter = user();
        Thread_ thread = thread();

        won(stake(bet(thread), punter, 100), 40);
        won(stake(bet(thread), punter, 50), 25);
        lost(stake(bet(thread), punter, 30));
        stake(bet(thread), punter, 10);   // still running

        PredictionRecord record = predictions.recordOf(punter.uid());

        assertThat(record.total()).isEqualTo(4);
        assertThat(record.settled())
                .as("the one still running is neither right nor wrong yet")
                .isEqualTo(3);
        assertThat(record.correct()).isEqualTo(2);
    }

    @Test
    @DisplayName("a refund is not a loss")
    void doesNotCountRefundsAgainstYou() {
        User_ punter = user();
        Thread_ thread = thread();

        won(stake(bet(thread), punter, 100), 40);
        refunded(stake(bet(thread), punter, 60));

        PredictionRecord record = predictions.recordOf(punter.uid());

        assertThat(record.total()).isEqualTo(2);
        assertThat(record.settled())
                .as("nobody backed the outcome, so every stake went back and the "
                        + "bet decided nothing - counting it as a loss would mark "
                        + "somebody down for a bet that never resolved")
                .isEqualTo(1);
        assertThat(record.correct()).isEqualTo(1);
    }

    @Test
    @DisplayName("says nothing about which threads they were")
    void carriesNoThreadDetail() {
        User_ punter = user();
        won(stake(bet(thread()), punter, 100), 40);

        // The whole reason this is a separate query. PredictionRecord has three
        // fields and none of them is a title, a description, an id or an amount -
        // if any is ever added, a prediction on a private thread starts naming it.
        assertThat(PredictionRecord.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .containsExactlyInAnyOrder("total", "settled", "correct");
    }

    @Test
    @DisplayName("a blocked viewer is told nothing")
    void refusesBlockedViewers() {
        User_ punter = user();
        User_ viewer = user();
        won(stake(bet(thread()), punter, 100), 40);

        String viewing = loginAs(viewer);

        // Readable first, so the refusal below is demonstrably the block rather
        // than the endpoint never having worked.
        assertThat(recordOf(punter, viewing).getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> blocked = rest.exchange("/api/blocks/" + viewer.uid(),
                HttpMethod.POST, new HttpEntity<>(cookie(loginAs(punter))), String.class);
        assertThat(blocked.getStatusCode().is2xxSuccessful())
                .as("the block has to have been applied, or the assertion below "
                        + "proves nothing")
                .isTrue();

        // 404 rather than 403, which is BlockService's own convention: telling
        // somebody they have been blocked is itself a message from a person who
        // chose to stop receiving them.
        assertThat(recordOf(punter, viewing).getStatusCode())
                .as("blocked means nothing of theirs is visible, the same as "
                        + "their threads")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<String> recordOf(User_ target, String viewerSession) {
        return rest.exchange("/api/predictions/record/" + target.uid(), HttpMethod.GET,
                new HttpEntity<>(cookie(viewerSession)), String.class);
    }

    @Test
    @DisplayName("somebody with no predictions is zero, not an error")
    void handlesSomebodyWhoHasNeverStaked() {
        PredictionRecord record = predictions.recordOf(user().uid());

        assertThat(record.total()).isZero();
        assertThat(record.settled())
                .as("the screen divides correct by settled; zero here has to come "
                        + "back as zero rather than as a null to divide by")
                .isZero();
        assertThat(record.correct()).isZero();
    }

    // --- helpers ----------------------------------------------------------

    private HttpHeaders cookie(String session) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, session);
        return headers;
    }

    private String loginAs(User_ user) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", user.user_name());
        form.add("password", "password");
        ResponseEntity<String> response =
                rest.postForEntity("/login", new HttpEntity<>(form, headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";")[0];
    }

    private void won(Prediction_ prediction, long profit) {
        predictions.updateAmountWon(prediction.pid(), profit);
    }

    private void lost(Prediction_ prediction) {
        predictions.updateAmountWon(prediction.pid(), -prediction.amount_bet());
    }

    private void refunded(Prediction_ prediction) {
        predictions.updateAmountWon(prediction.pid(), 0L);
    }

    private Prediction_ stake(Bet_ bet, User_ punter, long amount) {
        return predictions.save(new Prediction_(null, bet.bid(), punter.uid(), true,
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
        String name = "record-" + seq;
        return users.save(new User_(null, name, name + "@example.test",
                passwordEncoder.encode("password"),
                "+" + seq, null, true, "", null, new Date().getTime(), null,
                0, null, "offline", null, 0.0, null));
    }
}
