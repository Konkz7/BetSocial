package com.example.World;

import com.example.World.Groups.GroupService;
import com.example.World.Groups.Group_;
import com.example.World.Messages.MessageRepository;
import com.example.World.Messages.Message_;
import com.example.World.Users.UserRepository;
import com.example.World.Users.User_;
import com.example.World.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Locks in the Phase 3 security fixes.
 *
 * Each case here was verified by hand against a running server at the time. These
 * tests make that verification repeatable, so a future change cannot quietly
 * reopen any of them.
 *
 * Relies on Startup seeding admin/john/jane (all with the password "password")
 * into the empty container database.
 */
@DisplayName("Security regressions")
class SecurityRegressionTest extends AbstractIntegrationTest {

    // --- the role lockout -------------------------------------------------

    @Test
    @DisplayName("bets and predictions are reachable by an authenticated user")
    void betsAndPredictionsAreNotLockedOut() {
        // /api/bets/** and /api/predictions/** required hasRole("TEXT"), a role
        // UserService never grants - so these returned 403 to everyone, admins
        // included, making the core product API unreachable.
        String session = login("john", "password");

        assertThat(get("/api/bets/all", session).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get("/api/predictions/all", session).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("superuser endpoints admit an admin but refuse a plain user")
    void superuserEndpointsUseRealRoles() {
        // Previously hasAnyRole("IMAGE","ADMIN") - ROLE_IMAGE is never granted.
        assertThat(get("/superusers/all", login("admin", "password")).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(get("/superusers/all", login("john", "password")).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // --- the entity leak --------------------------------------------------

    @Test
    @DisplayName("no user-bearing response exposes credentials or tokens")
    void userResponsesAreProjected() {
        // Endpoints used to serialise User_ directly. The feed was worst:
        // ThreadProfile embedded a whole User_, so every scroll returned each
        // author's BCrypt hash and verification token.
        String session = login("john", "password");

        for (String path : new String[]{
                "/req/profile", "/api/users/all", "/api/threads/active"}) {
            String body = get(path, session).getBody();
            assertThat(body)
                    .as("%s must not leak sensitive fields", path)
                    .doesNotContain("pass_word")
                    .doesNotContain("verification_token")
                    .doesNotContain("fb_notification_token")
                    .doesNotContain("wallet_address");
        }
    }

    @Test
    @DisplayName("the profile still carries the fields the client renders")
    void projectionKeepsWhatTheClientNeeds() {
        String body = get("/req/profile", login("john", "password")).getBody();

        assertThat(body).contains("uid", "user_name", "bio", "profile_picture", "status");
    }

    // --- removed endpoints ------------------------------------------------

    @Test
    @DisplayName("unauthenticated hard-delete endpoints are gone")
    void hardDeleteEndpointsRemoved() {
        // These had no ownership check and bypassed the deleted_at soft-delete.
        String session = login("john", "password");

        for (String path : new String[]{
                "/api/threads/delete/1", "/api/bets/delete/1", "/api/predictions/delete/1"}) {
            assertThat(exchange(HttpMethod.DELETE, path, session).getStatusCode())
                    .as("%s should no longer exist", path)
                    .isIn(HttpStatus.NOT_FOUND, HttpStatus.METHOD_NOT_ALLOWED);
        }
    }

    @Test
    @DisplayName("the bulk message dump is gone")
    void bulkMessageDumpRemoved() {
        // GET /api/messages/all returned every message between every user.
        assertThat(get("/api/messages/all", login("john", "password")).getStatusCode())
                .isNotEqualTo(HttpStatus.OK);
    }

    // --- ownership --------------------------------------------------------

    @Test
    @DisplayName("a user cannot read a conversation they do not belong to")
    void conversationsRequireMembership() {
        String jane = login("jane", "password");

        // Group 1 does not exist for jane; membership is checked before anything else.
        ResponseEntity<String> response = get("/api/messages/group/1", jane);
        assertThat(response.getStatusCode())
                .isIn(HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("anonymous callers are refused")
    void anonymousIsRefused() {
        ResponseEntity<String> response = get("/api/users/all", null);
        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.OK);
    }

    // --- empty collections ------------------------------------------------

    @Test
    @DisplayName("a user with no conversations gets an empty list, not a 404")
    void emptyConversationListIsNotAnError() {
        // These threw 404 when the user had no groups, and the client turned that
        // into a "Groups couldnt be found." alert for anyone who had not yet
        // started a chat. No results is not an error.
        String session = login("jane", "password");

        for (String path : new String[]{"/api/groups/user-groups", "/api/groups/group-users"}) {
            ResponseEntity<String> response = get(path, session);
            assertThat(response.getStatusCode())
                    .as("%s should return 200 for a user with no conversations", path)
                    .isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .as("%s should return an empty JSON array", path)
                    .isEqualTo("[]");
        }
    }

    // --- login ------------------------------------------------------------

    @Test
    @DisplayName("returns a single parseable JSON object")
    void loginResponseIsValidJson() throws Exception {
        // The handler used to make two write() calls, producing
        //     {"message":"Login successful!"}{"userId":"2"}
        // which is not valid JSON and which no client could parse.
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", "jane");
        form.add("password", "password");

        ResponseEntity<String> response =
                rest.postForEntity("/login", new HttpEntity<>(form, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType())
                .as("the response should declare that it is JSON")
                .isNotNull();
        assertThat(response.getHeaders().getContentType().includes(MediaType.APPLICATION_JSON)).isTrue();

        JsonNode body = new ObjectMapper().readTree(response.getBody());
        assertThat(body.get("message").asText()).isEqualTo("Login successful!");
        assertThat(body.get("userId").asLong()).isPositive();
    }

    @Test
    @DisplayName("refuses a second login on the same session with 403, not 500")
    void secondLoginOnSameSessionIsRefused() {
        // The handler used to throw from onAuthenticationSuccess after setting 403,
        // which handed control to the container and turned it into a 500.
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", "mike");
        form.add("password", "password");

        ResponseEntity<String> first =
                rest.postForEntity("/login", new HttpEntity<>(form, headers), String.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Same session cookie, login again.
        headers.add(HttpHeaders.COOKIE, first.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";")[0]);
        ResponseEntity<String> second =
                rest.postForEntity("/login", new HttpEntity<>(form, headers), String.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("login accepts credentials in a form body, not just the URL")
    void loginAcceptsFormBody() {
        // Slice E moved credentials out of the query string; this proves the
        // server side of that contract.
        assertThat(login("john", "password")).isNotBlank();
    }

    // --- messaging authorization ------------------------------------------

    @Test
    @DisplayName("a message is only readable by the members of its conversation")
    void messageByIdRequiresMembership() {
        // GET /api/messages/{mid} took no session and checked nothing, so the
        // membership rule on /api/messages/group/{gid} could be sidestepped
        // entirely by walking mids one at a time.
        Conversation convo = conversation();

        assertThat(get("/api/messages/" + convo.mid(), loginAs(convo.member())).getStatusCode())
                .as("a member should still be able to read the message")
                .isEqualTo(HttpStatus.OK);

        assertThat(get("/api/messages/" + convo.mid(), loginAs(convo.outsider())).getStatusCode())
                .as("a non-member should not be able to read it by id")
                .isIn(HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("a non-member cannot mark a message read")
    void readReceiptRequiresMembership() {
        // PUT /api/messages/update-read/{mid} took no HttpSession parameter at
        // all - it was reachable by any authenticated caller for any message.
        Conversation convo = conversation();

        assertThat(exchange(HttpMethod.PUT, "/api/messages/update-read/" + convo.mid(),
                loginAs(convo.outsider())).getStatusCode())
                .as("a non-member should not be able to mark it read")
                .isIn(HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND);

        assertThat(exchange(HttpMethod.PUT, "/api/messages/update-read/" + convo.mid(),
                loginAs(convo.member())).getStatusCode())
                .as("the recipient should still be able to mark it read")
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("a group is only readable by its members")
    void groupByIdRequiresMembership() {
        Conversation convo = conversation();

        assertThat(get("/api/groups/" + convo.gid(), loginAs(convo.member())).getStatusCode())
                .as("a member should still be able to read the group")
                .isEqualTo(HttpStatus.OK);

        assertThat(get("/api/groups/" + convo.gid(), loginAs(convo.outsider())).getStatusCode())
                .as("a non-member should not be able to read it")
                .isIn(HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("the bulk group dump is gone")
    void bulkGroupDumpRemoved() {
        // GET /api/groups/all returned every group row in the database, which
        // includes the names of conversations the caller has nothing to do with.
        assertThat(get("/api/groups/all", login("john", "password")).getStatusCode())
                .isNotEqualTo(HttpStatus.OK);
    }

    // --- helpers ----------------------------------------------------------

    @Autowired UserRepository users;
    @Autowired GroupService groups;
    @Autowired MessageRepository messages;
    @Autowired PasswordEncoder passwordEncoder;

    // phone_number is globally unique and the suite shares one database, so each
    // test class seeds from its own block. Taken so far: 2_340 (predictions),
    // 2_344 (soft delete), 2_345 (feed visibility), 2_346 (feed query count),
    // 2_347 (thread profile), 2_348 (notifications), 2_349 (conversations).
    private static final AtomicLong AUTHZ_SEQ = new AtomicLong(2_341_000_000_000L);

    /** A DM between two users, one message in it, and an unrelated third user. */
    private record Conversation(User_ member, User_ peer, User_ outsider, Long gid, Long mid) {}

    /**
     * Builds the fixture from fresh users rather than the seeded john/jane, so
     * that it cannot disturb tests which assume a seeded user has no
     * conversations - emptyConversationListIsNotAnError being one of them.
     */
    private Conversation conversation() {
        User_ member = users.save(authzUser());
        User_ peer = users.save(authzUser());
        User_ outsider = users.save(authzUser());

        Group_ group = groups.openDirectConversation(member.uid(), peer.uid());

        // The row is written straight through the repository rather than through
        // MessageService.sendMessage. What is under test here is who may read a
        // message, not how one is sent, and going via the service would couple
        // this fixture to that method's signature for no benefit.
        Message_ message = messages.save(new Message_(
                null, peer.uid(), "private", 0,
                new Date().getTime(), null, group.gid(), false, null));

        return new Conversation(member, peer, outsider, group.gid(), message.mid());
    }

    private User_ authzUser() {
        long seq = AUTHZ_SEQ.incrementAndGet();
        String name = "authz-" + seq;
        return new User_(null, name, name + "@example.test", passwordEncoder.encode("password"),
                "+" + seq, null, true, "", null, new Date().getTime(), null,
                0, null, "offline", null, 0.0, null);
    }

    private String loginAs(User_ user) {
        return login(user.user_name(), "password");
    }

    /** Logs in with a form-encoded body and returns the session cookie. */
    private String login(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", username);
        form.add("password", password);

        ResponseEntity<String> response =
                rest.postForEntity("/login", new HttpEntity<>(form, headers), String.class);

        assertThat(response.getStatusCode())
                .as("login should succeed for %s", username)
                .isEqualTo(HttpStatus.OK);

        String cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(cookie).as("a session cookie should be issued").isNotNull();
        return cookie.split(";")[0];
    }

    private ResponseEntity<String> get(String path, String session) {
        return exchange(HttpMethod.GET, path, session);
    }

    private ResponseEntity<String> exchange(HttpMethod method, String path, String session) {
        HttpHeaders headers = new HttpHeaders();
        if (session != null) {
            headers.add(HttpHeaders.COOKIE, session);
        }
        return rest.exchange(path, method, new HttpEntity<>(headers), String.class);
    }
}
