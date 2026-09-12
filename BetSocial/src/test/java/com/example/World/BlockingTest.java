package com.example.World;

import com.example.World.Blocks.BlockRepository;
import com.example.World.Blocks.BlockService;
import com.example.World.Follows.FollowRepository;
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
 * Blocking, which had to exist before this could be released at all.
 *
 * Apple's guideline 1.2 requires any app carrying user-generated content to let
 * people block other users, and BetSocial had no way to do it.
 *
 * A block is mutual on purpose: whoever pressed the button, neither sees the
 * other. A one-way hide would leave the person you blocked still watching
 * everything you post, which is not what anybody means by the word.
 */
@DisplayName("Blocking")
class BlockingTest extends AbstractIntegrationTest {

    // See SecurityRegressionTest for the per-class phone_number blocks in use.
    private static final AtomicLong PHONE = new AtomicLong(2_356_000_000_000L);

    @Autowired UserRepository users;
    @Autowired ThreadRepository threads;
    @Autowired BlockRepository blocks;
    @Autowired BlockService blockService;
    @Autowired FollowRepository follows;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("a blocked author's thread leaves the feed")
    void blockedAuthorDisappearsFromTheFeed() {
        User_ viewer = user();
        User_ nuisance = user();
        Thread_ thread = threads.save(thread(nuisance, "something unwelcome"));

        String session = loginAs(viewer);
        assertThat(get("/api/threads/active", session).getBody())
                .as("visible before the block, or the test proves nothing")
                .contains("something unwelcome");

        block(session, nuisance);

        assertThat(get("/api/threads/active", session).getBody())
                .doesNotContain("something unwelcome");
    }

    @Test
    @DisplayName("and the blocker's threads leave theirs - it cuts both ways")
    void blockingIsMutual() {
        User_ blocker = user();
        User_ blocked = user();
        threads.save(thread(blocker, "posted by the blocker"));

        block(loginAs(blocker), blocked);

        assertThat(get("/api/threads/active", loginAs(blocked)).getBody())
                .as("a one-way hide would leave them watching everything you post")
                .doesNotContain("posted by the blocker");
    }

    @Test
    @DisplayName("a blocked author's thread cannot be reached by id either")
    void blockedThreadIsNotReadableById() {
        User_ viewer = user();
        User_ nuisance = user();
        Thread_ thread = threads.save(thread(nuisance, "reachable by number"));

        String session = loginAs(viewer);
        block(session, nuisance);

        assertThat(get("/api/threads/thread-profile/" + thread.tid(), session).getStatusCode())
                .as("the feed filter is not enough if the id still works")
                .isNotEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("blocking removes any follow between the two, both ways")
    void blockingUnfollows() {
        User_ blocker = user();
        User_ blocked = user();

        String blockerSession = loginAs(blocker);
        post("/api/follows/send/" + blocked.uid(), blockerSession);
        post("/api/follows/send/" + blocker.uid(), loginAs(blocked));
        assertThat(follows.existsByRequestIdAndReceiveId(blocker.uid(), blocked.uid())).isTrue();

        block(blockerSession, blocked);

        assertThat(follows.existsByRequestIdAndReceiveId(blocker.uid(), blocked.uid()))
                .as("left in place, a blocked person still forms the mutual follow "
                        + "that makes a private thread visible")
                .isFalse();
        assertThat(follows.existsByRequestIdAndReceiveId(blocked.uid(), blocker.uid())).isFalse();
    }

    @Test
    @DisplayName("a blocked person cannot follow you back")
    void blockedUserCannotFollow() {
        User_ blocker = user();
        User_ blocked = user();
        block(loginAs(blocker), blocked);

        ResponseEntity<String> response =
                post("/api/follows/send/" + blocker.uid(), loginAs(blocked));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("a blocked profile answers the same as one that does not exist")
    void blockedProfileIsNotFound() {
        User_ viewer = user();
        User_ nuisance = user();
        String session = loginAs(viewer);
        block(session, nuisance);

        assertThat(get("/api/users/" + nuisance.uid(), session).getStatusCode())
                .as("saying 'you are blocked' is itself a message from somebody "
                        + "who chose to stop sending them")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("blocking twice is the same block, not two")
    void blockingIsIdempotent() {
        User_ blocker = user();
        User_ blocked = user();
        String session = loginAs(blocker);

        block(session, blocked);
        block(session, blocked);

        assertThat(blocks.blockedBy(blocker.uid())).hasSize(1);
    }

    @Test
    @DisplayName("unblocking brings them back")
    void unblockRestoresVisibility() {
        User_ viewer = user();
        User_ other = user();
        threads.save(thread(other, "back again"));
        String session = loginAs(viewer);

        block(session, other);
        assertThat(get("/api/threads/active", session).getBody()).doesNotContain("back again");

        ResponseEntity<String> unblocked = exchange(HttpMethod.DELETE,
                "/api/blocks/" + other.uid(), session);
        assertThat(unblocked.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(get("/api/threads/active", session).getBody()).contains("back again");
    }

    @Test
    @DisplayName("nobody blocks themselves")
    void cannotBlockYourself() {
        User_ self = user();

        ResponseEntity<String> response =
                post("/api/blocks/" + self.uid(), loginAs(self));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("your block list is your own")
    void blockListIsPrivate() {
        User_ blocker = user();
        User_ blocked = user();
        User_ stranger = user();
        block(loginAs(blocker), blocked);

        assertThat(get("/api/blocks", loginAs(stranger)).getBody())
                .as("who you have blocked would tell a blocked person where they stand")
                .doesNotContain(blocked.user_name());
    }

    @Test
    @DisplayName("invisibleTo answers for both sides of the same row")
    void invisibleToReadsBothDirections() {
        User_ blocker = user();
        User_ blocked = user();
        block(loginAs(blocker), blocked);

        assertThat(blockService.invisibleTo(blocker.uid())).contains(blocked.uid());
        assertThat(blockService.invisibleTo(blocked.uid()))
                .as("the row is stored once and has to be read from both ends")
                .contains(blocker.uid());
    }

    // --- fixtures ----------------------------------------------------------

    private void block(String session, User_ target) {
        ResponseEntity<String> response = post("/api/blocks/" + target.uid(), session);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    private Thread_ thread(User_ owner, String title) {
        return new Thread_(null, owner.uid(), title, null, 0,
                "test", 0L, new Date().getTime(), null, false, null);
    }

    private User_ user() {
        long seq = PHONE.incrementAndGet();
        String name = "blocking-" + seq;
        return users.save(new User_(null, name, name + "@example.test",
                passwordEncoder.encode("password"),
                "+" + seq, null, true, "", null, new Date().getTime(), null,
                0, null, "offline", null, 0.0, null));
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

    private ResponseEntity<String> get(String path, String session) {
        return exchange(HttpMethod.GET, path, session);
    }

    private ResponseEntity<String> post(String path, String session) {
        return exchange(HttpMethod.POST, path, session);
    }

    private ResponseEntity<String> exchange(HttpMethod method, String path, String session) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, session);
        return rest.exchange(path, method, new HttpEntity<>(headers), String.class);
    }
}
