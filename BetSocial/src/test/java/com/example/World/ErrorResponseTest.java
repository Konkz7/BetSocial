package com.example.World;

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
 * That the advice did not turn ordinary failures into 500s.
 *
 * The risk in adding one is catching Exception and swallowing everything Spring
 * already handled correctly - a wrong method, malformed JSON, a missing
 * parameter. All of those have a right answer and it is not "the server broke".
 *
 * ApiErrorHandlerTest covers what a client is told. This covers what status it
 * is told it with.
 */
@DisplayName("Error responses, end to end")
class ErrorResponseTest extends AbstractIntegrationTest {

    // See SecurityRegressionTest for the per-class phone_number blocks in use.
    private static final AtomicLong PHONE = new AtomicLong(2_369_000_000_000L);

    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("the wrong method is still 405")
    void keepsMethodNotAllowed() {
        String session = loginAs(user());

        // my-data is a GET. A POST to it is the caller's mistake, not ours.
        //
        // Not GET /api/users/change-bio, which looks like the obvious case and is
        // not one: that path also matches the GET /{uid} route, and "change-bio"
        // failing to convert to a Long makes it a genuine 400.
        ResponseEntity<String> response = exchange(
                "/api/users/my-data", HttpMethod.POST, session, null);

        assertThat(response.getStatusCode())
                .as("an advice that catches Exception without extending "
                        + "ResponseEntityExceptionHandler turns every one of these into a 500")
                .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    @DisplayName("malformed JSON is still 400")
    void keepsBadRequestForUnreadableBodies() {
        String session = loginAs(user());

        ResponseEntity<String> response = exchange(
                "/api/threads/make", HttpMethod.POST, session, "{not json at all");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getStatusCode().is5xxServerError()).isFalse();
    }

    @Test
    @DisplayName("an author's reason still reaches the client")
    void passesThroughWrittenReasons() {
        String session = loginAs(user());

        // MediaReference is unconfigured in tests, so it accepts anything - this
        // uses the rate limiter instead, whose refusal carries a written reason
        // and is reachable without any setup.
        ResponseEntity<String> refused = null;
        for (int i = 0; i < 30 && (refused == null || refused.getStatusCode().value() != 429); i++) {
            refused = exchange("/api/users/my-data/link", HttpMethod.POST, session, null);
        }

        assertThat(refused.getStatusCode().value())
                .as("Limits.DATA_EXPORT allows ten an hour; thirty must not all pass")
                .isEqualTo(429);

        assertThat(refused.getBody())
                .as("a reason written for a person to read is the whole thing this "
                        + "advice exists to keep, and it is what the app displays")
                .contains("message")
                .isNotEqualTo("{}");
    }

    // --- helpers ----------------------------------------------------------

    private ResponseEntity<String> exchange(String path, HttpMethod method, String session, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, session);
        return rest.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }

    private User_ user() {
        long seq = PHONE.incrementAndGet();
        String name = "errors-" + seq;
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
}
