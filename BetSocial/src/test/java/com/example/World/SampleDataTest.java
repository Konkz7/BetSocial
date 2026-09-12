package com.example.World;

import com.example.World.Users.UserRepository;
import com.example.World.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Sample data runs when it is asked for, and only then.
 *
 * It writes a few hundred rows, which is exactly what is wanted when testing
 * pagination by hand and exactly what is not wanted anywhere else - so the
 * default matters more than the feature does.
 *
 * The endpoint exists because the property alone was not enough: it has to be
 * set before startup and does not reach an IDE run configuration, so switching
 * it on meant launching the backend a particular way and forgetting looked
 * identical to the feature being broken.
 */
@DisplayName("Sample data")
// Ordered on purpose: the generate test writes a few hundred rows into the
// container every class shares, so "off by default" has to be asked before
// anything has asked for it.
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SampleDataTest extends AbstractIntegrationTest {

    @Autowired UserRepository users;

    @Test
    @Order(1)
    @DisplayName("does not run unless asked")
    void isOffByDefault() {
        // The test context sets no sample-data property, which is the same path a
        // deployment takes. If the default ever changes, this fails first.
        assertThat(users.findByUsername("sample0"))
                .as("sample accounts must only exist when they have been asked for")
                .isEmpty();

        assertThat(sampleAccounts()).isZero();
    }

    @Test
    @Order(2)
    @DisplayName("an ordinary user cannot ask for it")
    void generatingIsPrivileged() {
        ResponseEntity<String> response = post("/superusers/sample-data", login("john"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(sampleAccounts())
                .as("a refused request must not have written anything")
                .isZero();
    }

    @Test
    @Order(3)
    @DisplayName("an admin can ask, and asking twice adds nothing the second time")
    void adminCanGenerateAndItIsIdempotent() {
        String admin = login("admin");

        try {
            assertThat(post("/superusers/sample-data", admin).getStatusCode())
                    .isEqualTo(HttpStatus.OK);

            // Deliberately no assertion that rows appeared. Every other test
            // class seeds its own fixtures into this container, so it already
            // holds more users and threads than the seeder's targets - which
            // means doing nothing is the correct behaviour here, and a count is
            // not evidence either way.
            //
            // What can be checked is the mistake this refactor introduced:
            // topUpConversation created a fresh conversation on every call,
            // which never mattered while it only ran once at startup and matters
            // a great deal now that asking is a button.
            post("/superusers/sample-data", admin);

            assertThat(conversationsNamed(CONVERSATION_NAME))
                    .as("asking twice must not leave two identical conversations")
                    .isLessThanOrEqualTo(1);
        } finally {
            // Put the container back. The sample conversation otherwise appears
            // in tests that assert a user has none - SecurityRegressionTest's
            // empty-conversation case caught exactly that, because the sample
            // chat is built from the first three accounts, which are the seeded
            // ones every other test logs in as.
            removeSampleData();
        }
    }

    // --- helpers ----------------------------------------------------------

    private static final String CONVERSATION_NAME = "Sample long chat";

    /**
     * Removes anything the seeder created, in foreign-key order.
     *
     * Deliberately thorough rather than clever: leaving one row behind means a
     * failure in an unrelated class, which is a far more expensive thing to
     * debug than these five statements are to read.
     */
    private void removeSampleData() {
        jdbc.update("""
                DELETE FROM Message_ WHERE gid IN
                    (SELECT gid FROM Group_ WHERE group_name = ?)""", CONVERSATION_NAME);
        jdbc.update("""
                DELETE FROM Groupuser_ WHERE gid IN
                    (SELECT gid FROM Group_ WHERE group_name = ?)""", CONVERSATION_NAME);
        jdbc.update("DELETE FROM Group_ WHERE group_name = ?", CONVERSATION_NAME);
        jdbc.update("""
                DELETE FROM Thread_ WHERE uid IN
                    (SELECT uid FROM User_ WHERE user_name LIKE 'sample%')""");
        jdbc.update("DELETE FROM User_ WHERE user_name LIKE 'sample%'");
    }

    private long sampleAccounts() {
        return count("SELECT COUNT(*) FROM User_ WHERE user_name LIKE 'sample%'");
    }

    private long conversationsNamed(String name) {
        Long found = jdbc.queryForObject(
                "SELECT COUNT(*) FROM Group_ WHERE group_name = ?", Long.class, name);
        return found == null ? 0 : found;
    }

    private long count(String sql) {
        Long found = jdbc.queryForObject(sql, Long.class);
        return found == null ? 0 : found;
    }

    private String login(String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", username);
        form.add("password", "password");

        ResponseEntity<String> response =
                rest.postForEntity("/login", new HttpEntity<>(form, headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";")[0];
    }

    private ResponseEntity<String> post(String path, String session) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, session);
        return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(headers), String.class);
    }
}
