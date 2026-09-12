package com.example.World;

import com.example.World.Bets.BetRepository;
import com.example.World.Bets.Bet_;
import com.example.World.Bets.Status;
import com.example.World.Comments.CommentRepository;
import com.example.World.Comments.Comment_;
import com.example.World.Predictions.PredictionRepository;
import com.example.World.Predictions.Prediction_;
import com.example.World.Reports.ReportRepository;
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
 * Reporting content, and a moderator acting on it.
 *
 * The other half of Apple's guideline 1.2. Blocking is a private act by one
 * person; this is the path that reaches somebody who can take the material down
 * for everybody.
 */
@DisplayName("Reporting")
class ReportingTest extends AbstractIntegrationTest {

    // See SecurityRegressionTest for the per-class phone_number blocks in use.
    private static final AtomicLong PHONE = new AtomicLong(2_357_000_000_000L);

    @Autowired UserRepository users;
    @Autowired ThreadRepository threads;
    @Autowired CommentRepository comments;
    @Autowired BetRepository bets;
    @Autowired PredictionRepository predictions;
    @Autowired ReportRepository reports;
    @Autowired LedgerService ledger;
    @Autowired PasswordEncoder passwordEncoder;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("a reported thread reaches the moderation queue")
    void reportedThreadReachesTheQueue() {
        User_ author = user();
        User_ reporter = user();
        Thread_ thread = threads.save(thread(author, "something against the rules"));

        assertThat(report(loginAs(reporter), "THREAD", thread.tid(), "ABUSE").getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        assertThat(get("/superusers/reports", adminSession()).getBody())
                .contains("something against the rules")
                .contains(author.user_name());
    }

    @Test
    @DisplayName("the same person cannot report the same thing twice")
    void reportingTwiceIsRefused() {
        User_ reporter = user();
        Thread_ thread = threads.save(thread(user(), "reported once"));
        String session = loginAs(reporter);

        assertThat(report(session, "THREAD", thread.tid(), "SPAM").getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(report(session, "THREAD", thread.tid(), "SPAM").getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("reporting something that does not exist is refused")
    void reportingNothingIsRefused() {
        assertThat(report(loginAs(user()), "THREAD", 99_999_999L, "SPAM").getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("the queue is not readable by an ordinary user")
    void queueIsPrivileged() {
        assertThat(get("/superusers/reports", loginAs(user())).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("an ordinary user cannot decide a report")
    void decidingIsPrivileged() {
        Thread_ thread = threads.save(thread(user(), "not yours to judge"));
        report(loginAs(user()), "THREAD", thread.tid(), "ABUSE");
        long rid = reports.open().get(reports.open().size() - 1).rid();

        assertThat(post("/superusers/reports/decide", loginAs(user()),
                Map.of("rid", rid, "action", "REMOVED")).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("removing a thread takes it down and refunds every stake")
    void removingAThreadRefundsStakes() {
        User_ author = user();
        User_ backer = user();
        Thread_ thread = threads.save(thread(author, "a thread with money on it"));
        Bet_ bet = bets.save(bet(thread));
        predictions.save(new Prediction_(null, bet.bid(), backer.uid(), true, 200L,
                null, new Date().getTime(), null, null));
        ledger.record(backer.uid(), -200, com.example.World.Wallet.LedgerReason.STAKE,
                bet.bid(), "Stake");
        assertThat(ledger.balanceOf(backer.uid())).isEqualTo(800);

        report(loginAs(user()), "THREAD", thread.tid(), "ABUSE");
        long rid = openRidFor("THREAD", thread.tid());

        assertThat(post("/superusers/reports/decide", adminSession(),
                Map.of("rid", rid, "action", "REMOVED")).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(threads.findById(thread.tid()).orElseThrow().deleted_at())
                .as("a soft delete, so a wrong call is reversible")
                .isNotNull();
        assertThat(bets.findById(bet.bid()).orElseThrow().status())
                .isEqualTo(Status.CANCELLED.toInt());
        assertThat(ledger.balanceOf(backer.uid()))
                .as("they staked in good faith; the thread being removed is nothing to do with them")
                .isEqualTo(1_000);
    }

    @Test
    @DisplayName("removing a comment leaves the thread alone")
    void removingAComment() {
        User_ author = user();
        Thread_ thread = threads.save(thread(author, "host thread"));
        Comment_ comment = comments.save(new Comment_(null, thread.tid(), author.uid(), null,
                "something unpleasant", 0L, new Date().getTime(), null, null));

        report(loginAs(user()), "COMMENT", comment.cid(), "HARASSMENT");
        post("/superusers/reports/decide", adminSession(),
                Map.of("rid", openRidFor("COMMENT", comment.cid()), "action", "REMOVED"));

        assertThat(comments.findById(comment.cid()).orElseThrow().deleted_at()).isNotNull();
        assertThat(threads.findById(thread.tid()).orElseThrow().deleted_at()).isNull();
    }

    @Test
    @DisplayName("suspending an account stops it signing in and takes its posts down")
    void suspendingAnAccount() {
        User_ offender = user();
        Thread_ thread = threads.save(thread(offender, "posted by a suspended account"));

        report(loginAs(user()), "USER", offender.uid(), "ABUSE");
        assertThat(post("/superusers/reports/decide", adminSession(),
                Map.of("rid", openRidFor("USER", offender.uid()), "action", "SUSPENDED"))
                .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(users.findByUsername(offender.user_name()))
                .as("every lookup filters deleted_at, including the one login uses")
                .isEmpty();
        assertThat(threads.findById(thread.tid()).orElseThrow().deleted_at())
                .as("suspending the person while publishing their posts is not a suspension")
                .isNotNull();

        ResponseEntity<String> login = rest.postForEntity("/login",
                new HttpEntity<>(form(offender.user_name()), formHeaders()), String.class);
        assertThat(login.getStatusCode()).isNotEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("dismissing leaves the content where it is")
    void dismissingChangesNothing() {
        Thread_ thread = threads.save(thread(user(), "perfectly fine"));
        report(loginAs(user()), "THREAD", thread.tid(), "SPAM");

        post("/superusers/reports/decide", adminSession(),
                Map.of("rid", openRidFor("THREAD", thread.tid()), "action", "DISMISSED"));

        assertThat(threads.findById(thread.tid()).orElseThrow().deleted_at()).isNull();
    }

    @Test
    @DisplayName("one decision closes every report about the same thing")
    void oneDecisionClosesDuplicateReports() {
        Thread_ thread = threads.save(thread(user(), "reported by a crowd"));
        report(loginAs(user()), "THREAD", thread.tid(), "SPAM");
        report(loginAs(user()), "THREAD", thread.tid(), "ABUSE");
        report(loginAs(user()), "THREAD", thread.tid(), "OTHER");

        post("/superusers/reports/decide", adminSession(),
                Map.of("rid", openRidFor("THREAD", thread.tid()), "action", "REMOVED"));

        assertThat(reports.open().stream()
                .filter(r -> r.target_type().equals("THREAD") && r.target_id().equals(thread.tid())))
                .as("three people reporting one thread is one decision, not three")
                .isEmpty();
    }

    @Test
    @DisplayName("a report cannot be decided twice")
    void decidingTwiceIsRefused() {
        Thread_ thread = threads.save(thread(user(), "decided already"));
        report(loginAs(user()), "THREAD", thread.tid(), "SPAM");
        long rid = openRidFor("THREAD", thread.tid());
        String admin = adminSession();

        post("/superusers/reports/decide", admin, Map.of("rid", rid, "action", "DISMISSED"));

        assertThat(post("/superusers/reports/decide", admin,
                Map.of("rid", rid, "action", "REMOVED")).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("suspending is only for a report about a person")
    void suspendNeedsAUserReport() {
        Thread_ thread = threads.save(thread(user(), "not a person"));
        report(loginAs(user()), "THREAD", thread.tid(), "SPAM");

        assertThat(post("/superusers/reports/decide", adminSession(),
                Map.of("rid", openRidFor("THREAD", thread.tid()), "action", "SUSPENDED"))
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("nobody reports themselves")
    void cannotReportYourself() {
        User_ self = user();
        assertThat(report(loginAs(self), "USER", self.uid(), "SPAM").getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // --- fixtures ----------------------------------------------------------

    private long openRidFor(String type, Long id) {
        return reports.open().stream()
                .filter(r -> r.target_type().equals(type) && r.target_id().equals(id))
                .findFirst()
                .orElseThrow()
                .rid();
    }

    private ResponseEntity<String> report(String session, String type, Long id, String reason) {
        return post("/api/reports", session,
                Map.of("target_type", type, "target_id", id, "reason", reason,
                        "detail", "reported by a test"));
    }

    private Thread_ thread(User_ owner, String title) {
        return new Thread_(null, owner.uid(), title, null, 0,
                "test", 0L, new Date().getTime(), null, false, null);
    }

    private Bet_ bet(Thread_ thread) {
        long now = new Date().getTime();
        return new Bet_(null, thread.tid(), Status.ACTIVE.toInt(), null, 200L, 0L,
                "will it happen", now, null, now + 86_400_000L,
                false, false, false, 0L, 0L, null);
    }

    private User_ user() {
        long seq = PHONE.incrementAndGet();
        String name = "reporting-" + seq;
        User_ saved = users.save(new User_(null, name, name + "@example.test",
                passwordEncoder.encode("password"),
                "+" + seq, null, true, "", null, new Date().getTime(), null,
                0, null, "offline", null, 0.0, null));
        ledger.grantOpeningBalance(saved.uid());
        return saved;
    }

    private String loginAs(User_ user) {
        return login(user.user_name());
    }

    private String adminSession() {
        return login("admin");
    }

    private String login(String username) {
        ResponseEntity<String> response = rest.postForEntity("/login",
                new HttpEntity<>(form(username), formHeaders()), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";")[0];
    }

    private HttpHeaders formHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }

    private MultiValueMap<String, String> form(String username) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", username);
        form.add("password", "password");
        return form;
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
