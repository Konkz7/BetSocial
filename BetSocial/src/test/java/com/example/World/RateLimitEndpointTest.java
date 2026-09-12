package com.example.World;

import com.example.World.Users.UserRepository;
import com.example.World.Users.User_;
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
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * That the limiter is actually attached to anything.
 *
 * RateLimitTest covers the counting. This covers the part that is easy to get
 * wrong and impossible to notice: a limiter that works perfectly and is wired to
 * nothing looks exactly like a limiter that is working.
 */
@DisplayName("Rate limiting, in place")
class RateLimitEndpointTest extends AbstractIntegrationTest {

    // See SecurityRegressionTest for the per-class phone_number blocks in use.
    private static final AtomicLong PHONE = new AtomicLong(2_362_000_000_000L);

    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("posting threads is limited")
    void threadsAreLimited() {
        String session = loginAs(user());

        // Limits.THREADS allows 15 an hour; 25 attempts must not all succeed.
        long refused = IntStream.range(0, 25)
                .mapToObj(i -> post("/api/threads/make", session, Map.of(
                        "title", "spam " + i, "media", "", "media_type", 0,
                        "is_private", false, "category", "Tech")))
                .filter(response -> response.getStatusCode().value() == 429)
                .count();

        assertThat(refused)
                .as("unlimited posting is what makes a social app a spam target on day one")
                .isPositive();
    }

    @Test
    @DisplayName("one person running out does not affect anybody else")
    void limitsArePerPerson() {
        String heavy = loginAs(user());
        IntStream.range(0, 25).forEach(i -> post("/api/threads/make", heavy, Map.of(
                "title", "spam " + i, "media", "", "media_type", 0,
                "is_private", false, "category", "Tech")));

        String innocent = loginAs(user());
        ResponseEntity<String> response = post("/api/threads/make", innocent, Map.of(
                "title", "a perfectly normal thread", "media", "", "media_type", 0,
                "is_private", false, "category", "Tech"));

        assertThat(response.getStatusCode().value())
                .as("a shared allowance would let one script silence everybody")
                .isNotEqualTo(429);
    }

    @Test
    @DisplayName("posting and commenting have separate allowances")
    void actionsDoNotShareAnAllowance() {
        User_ author = user();
        String session = loginAs(author);

        // Spend the thread allowance.
        IntStream.range(0, 25).forEach(i -> post("/api/threads/make", session, Map.of(
                "title", "spam " + i, "media", "", "media_type", 0,
                "is_private", false, "category", "Tech")));

        // Commenting needs a thread; this one is somebody else's so it is not
        // affected by the author's spent allowance.
        ResponseEntity<String> comment = post("/api/comments/make", session, Map.of(
                "tid", 1, "parent_cid", 0, "description", "a comment"));

        assertThat(comment.getStatusCode().value())
                .as("scoping by user alone would make running out of one limit "
                        + "run out of all of them")
                .isNotEqualTo(429);
    }

    @Test
    @DisplayName("signing in successfully is never limited")
    void successfulLoginsAreNotCounted() {
        User_ person = user();

        // Well past the ten-failure allowance. None of these are failures.
        IntStream.range(0, 15).forEach(i -> loginAs(person));

        assertThat(login(person.user_name(), "password").getStatusCode())
                .as("counting attempts rather than failures would lock out "
                        + "anybody who simply signs in often")
                .isEqualTo(HttpStatus.OK);
    }

    // --- helpers ----------------------------------------------------------

    private User_ user() {
        long seq = PHONE.incrementAndGet();
        String name = "ratelimit-" + seq;
        return users.save(new User_(null, name, name + "@example.test",
                passwordEncoder.encode("password"),
                "+" + seq, null, true, "", null, new Date().getTime(), null,
                0, null, "offline", null, 0.0, null));
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
