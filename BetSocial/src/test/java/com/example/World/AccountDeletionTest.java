package com.example.World;

import com.example.World.Comments.CommentRepository;
import com.example.World.Comments.Comment_;
import com.example.World.Follows.FollowRepository;
import com.example.World.Threads.ThreadRepository;
import com.example.World.Threads.Thread_;
import com.example.World.Users.UserRepository;
import com.example.World.Users.UserView;
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
 * Deleting an account, and getting a copy of it first.
 *
 * The endpoint this replaces hard-deleted the row. Almost every table
 * referencing user_ cascades, so it took the person's threads, comments,
 * messages, predictions, follows - and their ledger entries, which are what
 * other people's settled payouts were computed from. It also failed outright
 * with a constraint violation for anybody who had ever decided a bet outcome,
 * because decision_log does not cascade.
 *
 * What happens instead: the person is scrubbed out and the content stays,
 * attributed to a tombstone.
 */
@DisplayName("Account deletion")
class AccountDeletionTest extends AbstractIntegrationTest {

    // See SecurityRegressionTest for the per-class phone_number blocks in use.
    private static final AtomicLong PHONE = new AtomicLong(2_363_000_000_000L);

    @Autowired UserRepository users;
    @Autowired ThreadRepository threads;
    @Autowired CommentRepository comments;
    @Autowired FollowRepository follows;
    @Autowired LedgerService ledger;
    @Autowired PasswordEncoder passwordEncoder;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("personal data is gone, the account row is not")
    void personalDataIsScrubbed() {
        User_ person = user();
        String session = loginAs(person);

        assertThat(deleteAccount(session, "password").getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        User_ after = users.findById(person.uid()).orElseThrow();
        assertThat(after.deleted_at()).isNotNull();
        assertThat(after.email()).doesNotContain(person.email());
        assertThat(after.phone_number()).isNotEqualTo(person.phone_number());
        assertThat(after.user_name()).isNotEqualTo(person.user_name());
        assertThat(after.fb_notification_token()).isNull();
        assertThat(after.profile_picture()).isNull();
        assertThat(after.bio()).isEmpty();
    }

    @Test
    @DisplayName("the row has to stay, because everything they wrote points at it")
    void theAccountRowSurvives() {
        User_ person = user();
        Thread_ thread = threads.save(thread(person, "something they wrote"));
        ledger.grantOpeningBalance(person.uid());

        deleteAccount(loginAs(person), "password");

        assertThat(users.findById(person.uid()))
                .as("hard-deleting cascades through eleven tables including the ledger")
                .isPresent();
        assertThat(threads.findById(thread.tid()).orElseThrow().deleted_at()).isNull();
        assertThat(ledger.balanceOf(person.uid()))
                .as("a settled bet's payout was computed from pools this stake was "
                        + "part of; removing it makes other balances unexplainable")
                .isEqualTo(1_000);
    }

    @Test
    @DisplayName("their content stays visible, under a tombstone")
    void contentIsAttributedToATombstone() {
        User_ author = user();
        User_ viewer = user();
        Thread_ thread = threads.save(thread(author, "still readable afterwards"));

        deleteAccount(loginAs(author), "password");

        String feed = get("/api/threads/active", loginAs(viewer)).getBody();
        assertThat(feed)
                .as("erasure covers personal data, not every trace somebody existed")
                .contains("still readable afterwards")
                .contains(UserView.TOMBSTONE_NAME)
                .doesNotContain(author.user_name());
    }

    @Test
    @DisplayName("a deleted account cannot sign in")
    void deletedAccountCannotSignIn() {
        User_ person = user();
        deleteAccount(loginAs(person), "password");

        assertThat(login(person.user_name(), "password").getStatusCode())
                .isNotEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("the wrong password does not delete anything")
    void wrongPasswordIsRefused() {
        User_ person = user();

        assertThat(deleteAccount(loginAs(person), "not-the-password").getStatusCode())
                .as("a borrowed phone should not be enough to delete an account")
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(users.findById(person.uid()).orElseThrow().deleted_at()).isNull();
    }

    @Test
    @DisplayName("follows are removed in both directions")
    void followsAreRemoved() {
        User_ leaver = user();
        User_ other = user();
        post("/api/follows/send/" + other.uid(), loginAs(leaver));
        post("/api/follows/send/" + leaver.uid(), loginAs(other));

        deleteAccount(loginAs(leaver), "password");

        assertThat(follows.existsByRequestIdAndReceiveId(leaver.uid(), other.uid())).isFalse();
        assertThat(follows.existsByRequestIdAndReceiveId(other.uid(), leaver.uid()))
                .as("a deleted account sitting in somebody's follower count is noise")
                .isFalse();
    }

    @Test
    @DisplayName("comments they left on other people's threads stay")
    void commentsSurvive() {
        User_ author = user();
        User_ commenter = user();
        Thread_ thread = threads.save(thread(author, "somebody else's thread"));
        Comment_ comment = comments.save(new Comment_(null, thread.tid(), commenter.uid(), null,
                "part of a discussion", 0L, new Date().getTime(), null, null));

        deleteAccount(loginAs(commenter), "password");

        assertThat(comments.findById(comment.cid()).orElseThrow().deleted_at())
                .as("deleting it would erase part of somebody else's thread")
                .isNull();
    }

    @Test
    @DisplayName("deleting twice is refused rather than repeated")
    void deletingTwiceIsRefused() {
        User_ person = user();
        String session = loginAs(person);

        assertThat(deleteAccount(session, "password").getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        // The session is invalidated by the first call, so this needs a fresh one -
        // and it cannot get one, which is itself the second line of defence.
        assertThat(deleteAccount(session, "password").getStatusCode())
                .isNotEqualTo(HttpStatus.NO_CONTENT);
    }

    // --- export -----------------------------------------------------------

    @Test
    @DisplayName("the export carries what is held about the caller")
    void exportContainsTheirData() {
        User_ person = user();
        threads.save(thread(person, "a thread in the export"));
        ledger.grantOpeningBalance(person.uid());

        String body = get("/api/users/my-data", loginAs(person)).getBody();

        assertThat(body)
                .contains(person.user_name())
                .contains(person.email())
                .contains("a thread in the export")
                .contains("OPENING_GRANT");
    }

    @Test
    @DisplayName("the export does not include the password hash")
    void exportOmitsCredentials() {
        User_ person = user();

        assertThat(get("/api/users/my-data", loginAs(person)).getBody())
                .as("handing somebody their own hash only helps whoever took their session")
                .doesNotContain("pass_word")
                .doesNotContain("$2a$");
    }

    @Test
    @DisplayName("the export needs a session")
    void exportNeedsASession() {
        ResponseEntity<String> response = rest.exchange("/api/users/my-data",
                HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);

        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.OK);
    }

    // --- fixtures ---------------------------------------------------------

    private Thread_ thread(User_ owner, String title) {
        return new Thread_(null, owner.uid(), title, null, 0,
                "test", 0L, new Date().getTime(), null, false, null);
    }

    private User_ user() {
        long seq = PHONE.incrementAndGet();
        String name = "deletion-" + seq;
        return users.save(new User_(null, name, name + "@example.test",
                passwordEncoder.encode("password"),
                "+" + seq, null, true, "a bio", null, new Date().getTime(), null,
                0, "a-push-token-" + seq, "offline", null, 0.0, null));
    }

    private String loginAs(User_ user) {
        ResponseEntity<String> response = login(user.user_name(), "password");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";")[0];
    }

    private ResponseEntity<String> login(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", username);
        form.add("password", password);
        return rest.postForEntity("/login", new HttpEntity<>(form, headers), String.class);
    }

    private ResponseEntity<String> deleteAccount(String session, String password) {
        return exchange(HttpMethod.DELETE, "/api/users/delete", session,
                Map.of("pass_word", password));
    }

    private ResponseEntity<String> post(String path, String session) {
        return exchange(HttpMethod.POST, path, session, null);
    }

    private ResponseEntity<String> get(String path, String session) {
        return exchange(HttpMethod.GET, path, session, null);
    }

    private ResponseEntity<String> exchange(HttpMethod method, String path,
                                            String session, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, session);
        try {
            HttpEntity<String> entity;
            if (body == null) {
                entity = new HttpEntity<>(headers);
            } else {
                headers.setContentType(MediaType.APPLICATION_JSON);
                entity = new HttpEntity<>(mapper.writeValueAsString(body), headers);
            }
            return rest.exchange(path, method, entity, String.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
