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
 * How the handshake refuses somebody.
 *
 * /ws has needed a session since WebSocket identity stopped being taken from a
 * client header, and that part works. What did not was the refusal: formLogin
 * answered an unauthenticated handshake with a 302 to /login, and a redirect is
 * not something an upgrade request can act on. The client waits for 101, gets a
 * redirect to an HTML page, and neither connects nor fails - it hangs on
 * "Opening Web Socket..." and retries every five seconds in silence.
 *
 * A status code is not usually worth a test. This one is, because the difference
 * between 401 and 302 here is the difference between an error somebody can read
 * and a socket that appears to be taking a long time.
 */
@DisplayName("WebSocket handshake")
class WebSocketHandshakeTest extends AbstractIntegrationTest {

    // See SecurityRegressionTest for the per-class phone_number blocks in use.
    private static final AtomicLong PHONE = new AtomicLong(2_368_000_000_000L);

    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("refuses an unauthenticated handshake with 401, not a redirect")
    void refusesWithAStatusTheClientCanRead() {
        ResponseEntity<String> response = handshake(null);

        assertThat(response.getStatusCode())
                .as("a 302 to an HTML login page is uninterpretable to a WebSocket "
                        + "client, which is why this looked like a hang rather than a refusal")
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        assertThat(response.getStatusCode().is3xxRedirection()).isFalse();
    }

    @Test
    @DisplayName("a signed-in caller gets past authorisation")
    void letsAMemberThrough() {
        ResponseEntity<String> response = handshake(loginAs(user()));

        // Not asserting 101: this is a plain GET rather than a real upgrade, so
        // the handshake handler rejects it on its own terms. The point is that it
        // reached the handler at all instead of being turned away by security.
        assertThat(response.getStatusCode())
                .as("the session is what /ws authorises on")
                .isNotEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getStatusCode().is3xxRedirection()).isFalse();
    }

    // --- helpers ----------------------------------------------------------

    private ResponseEntity<String> handshake(String session) {
        HttpHeaders headers = new HttpHeaders();
        if (session != null) {
            headers.add(HttpHeaders.COOKIE, session);
        }
        return rest.exchange("/ws", HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private User_ user() {
        long seq = PHONE.incrementAndGet();
        String name = "handshake-" + seq;
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
