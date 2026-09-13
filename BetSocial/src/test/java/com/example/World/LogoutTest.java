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
 * Logging out when you were not logged in.
 *
 * This is the common case rather than the odd one: the login screen posts
 * /logout every time it gains focus, so most calls arrive with no session at
 * all. CustomLogoutSuccessHandler read the principal off a null Authentication,
 * which wrote a NullPointerException and a full stack trace to the log on every
 * visit to that screen and answered the request with a 500.
 *
 * Two things were wrong and only one of them was noisy. A stack trace per
 * navigation buries whatever else is in the log - but a 500 also tells the app
 * that signing out failed, when it had already happened.
 */
@DisplayName("Logging out")
class LogoutTest extends AbstractIntegrationTest {

    // See SecurityRegressionTest for the per-class phone_number blocks in use.
    private static final AtomicLong PHONE = new AtomicLong(2_376_000_000_000L);

    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("without a session, it succeeds quietly")
    void succeedsWithNoSession() {
        ResponseEntity<String> response = rest.exchange("/logout", HttpMethod.POST,
                new HttpEntity<>(new HttpHeaders()), String.class);

        assertThat(response.getStatusCode())
                .as("logging out when you are not logged in is not an error - it is "
                        + "already true, and the login screen asks for it on every focus")
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("with a session, it still ends it")
    void endsARealSession() {
        User_ person = user();
        String session = loginAs(person);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, session);

        assertThat(rest.exchange("/logout", HttpMethod.POST,
                new HttpEntity<>(headers), String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        // The session is gone, so anything behind it is refused. Without this the
        // test above could be satisfied by a handler that never ends anything.
        ResponseEntity<String> afterwards = rest.exchange("/api/users/my-data",
                HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(afterwards.getStatusCode()).isNotEqualTo(HttpStatus.OK);
    }

    // --- helpers ----------------------------------------------------------

    private User_ user() {
        long seq = PHONE.incrementAndGet();
        String name = "logout-" + seq;
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
