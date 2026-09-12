package com.example.World;

import com.example.World.Bets.BetRepository;
import com.example.World.Bets.Bet_;
import com.example.World.Bets.Status;
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
 * The step between a bet closing and an approver seeing it.
 *
 * A closed bet was invisible to everybody. The sweep moved it to PENDING, which
 * took it off the staking screen; the approval queue filters on
 * outcome IS NOT NULL, which kept it out of there too. Its owner was the only
 * person who could move it on and nothing ever told them so - bets closed and
 * were simply never mentioned again.
 */
@DisplayName("Owner decision")
class OwnerDecisionTest extends AbstractIntegrationTest {

    // See SecurityRegressionTest for the per-class phone_number blocks in use.
    private static final AtomicLong PHONE = new AtomicLong(2_355_000_000_000L);

    @Autowired UserRepository users;
    @Autowired ThreadRepository threads;
    @Autowired BetRepository bets;
    @Autowired LedgerService ledger;
    @Autowired PasswordEncoder passwordEncoder;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("a closed bet is listed for its owner to decide")
    void closedBetAppearsForItsOwner() {
        User_ owner = user();
        Bet_ bet = closedBet(owner);

        ResponseEntity<String> response =
                get("/api/bets/awaiting-my-decision", loginAs(owner));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .as("the owner is the only person who can move this bet on")
                .contains("\"bid\":" + bet.bid())
                .contains("will it happen");
    }

    @Test
    @DisplayName("somebody else's closed bet is not")
    void closedBetIsNotListedForAnyoneElse() {
        User_ owner = user();
        Bet_ bet = closedBet(owner);
        User_ stranger = user();

        ResponseEntity<String> response =
                get("/api/bets/awaiting-my-decision", loginAs(stranger));

        assertThat(response.getBody()).doesNotContain("\"bid\":" + bet.bid());
    }

    @Test
    @DisplayName("a bet that is still running is not waiting on anybody")
    void activeBetIsNotListed() {
        User_ owner = user();
        Bet_ bet = bets.save(betOn(threads.save(thread(owner)), Status.ACTIVE.toInt()));

        ResponseEntity<String> response =
                get("/api/bets/awaiting-my-decision", loginAs(owner));

        assertThat(response.getBody())
                .as("it has not closed, so there is nothing to declare yet")
                .doesNotContain("\"bid\":" + bet.bid());
    }

    @Test
    @DisplayName("declaring an outcome hands the bet to the approval queue")
    void decidingMovesItToTheApprovers() {
        User_ owner = user();
        Bet_ bet = closedBet(owner);
        String session = loginAs(owner);

        ResponseEntity<String> decided = post("/api/bets/decide", session,
                Map.of("bid", bet.bid(), "decision", true, "reason", "it happened"));
        assertThat(decided.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(bets.findById(bet.bid()).orElseThrow().outcome()).isTrue();

        // Gone from the owner's list, arrived in the approver's - the two are
        // complementary, so a bet is never in both and never in neither.
        assertThat(get("/api/bets/awaiting-my-decision", session).getBody())
                .doesNotContain("\"bid\":" + bet.bid());
        assertThat(get("/superusers/bets/pending", adminSession()).getBody())
                .contains("\"bid\":" + bet.bid());
    }

    @Test
    @DisplayName("only the owner may declare the outcome")
    void strangerCannotDecide() {
        User_ owner = user();
        Bet_ bet = closedBet(owner);
        User_ stranger = user();

        ResponseEntity<String> response = post("/api/bets/decide", loginAs(stranger),
                Map.of("bid", bet.bid(), "decision", true, "reason", "nothing to do with me"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(bets.findById(bet.bid()).orElseThrow().outcome()).isNull();
    }

    @Test
    @DisplayName("the list needs a session")
    void loggedOutIsRefused() {
        User_ owner = user();
        Bet_ bet = closedBet(owner);

        ResponseEntity<String> response =
                rest.exchange("/api/bets/awaiting-my-decision", HttpMethod.GET,
                        new HttpEntity<>(new HttpHeaders()), String.class);

        // Not a specific code: the security chain redirects an anonymous caller to
        // the login form rather than answering 401, so requireUserId never runs.
        // What matters is that nothing comes back. Same assertion as
        // SecurityRegressionTest.anonymousIsRefused, for the same reason.
        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.OK);
        assertThat(response.getBody() == null ? "" : response.getBody())
                .as("a redirect body must not carry the bets anyway")
                .doesNotContain("\"bid\":" + bet.bid());
    }

    // --- fixtures ----------------------------------------------------------

    /** A bet whose time is up and which nobody has declared an outcome for. */
    private Bet_ closedBet(User_ owner) {
        return bets.save(betOn(threads.save(thread(owner)), Status.PENDING.toInt()));
    }

    private Thread_ thread(User_ owner) {
        return new Thread_(null, owner.uid(), "a thread", null, 0,
                "test", 0L, new Date().getTime(), null, false, null);
    }

    private Bet_ betOn(Thread_ thread, int status) {
        long now = new Date().getTime();
        return new Bet_(null, thread.tid(), status, null, 0L, 0L,
                "will it happen", now, null, now + 86_400_000L,
                false, false, false, 0L, 0L, null);
    }

    private User_ user() {
        long seq = PHONE.incrementAndGet();
        String name = "owner-decision-" + seq;
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

    private String adminSession() {
        return login("admin", "password");
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

    private ResponseEntity<String> get(String path, String session) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, session);
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private ResponseEntity<String> post(String path, String session, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, session);
        try {
            return rest.exchange(path, HttpMethod.POST,
                    new HttpEntity<>(mapper.writeValueAsString(body), headers), String.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
