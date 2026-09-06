package com.example.World;

import com.example.World.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

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
    @DisplayName("login accepts credentials in a form body, not just the URL")
    void loginAcceptsFormBody() {
        // Slice E moved credentials out of the query string; this proves the
        // server side of that contract.
        assertThat(login("john", "password")).isNotBlank();
    }

    // --- helpers ----------------------------------------------------------

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
