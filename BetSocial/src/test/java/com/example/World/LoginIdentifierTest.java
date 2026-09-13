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
 * What you can type into the login field.
 *
 * The screen is labelled Email, types with an email keyboard, holds the value in
 * a variable called email, and the forgot-password button beside it sends a real
 * message to whatever is in that box. Authentication, meanwhile, matched on
 * user_name alone - so the one thing the screen asked for was the one thing it
 * would not accept, and the same field meant two different things depending on
 * which button you pressed.
 *
 * Both columns are unique, so either can identify an account. These tests pin
 * that both are accepted, and that the tie between them resolves the same way
 * every time.
 */
@DisplayName("Signing in")
class LoginIdentifierTest extends AbstractIntegrationTest {

    // See SecurityRegressionTest for the per-class phone_number blocks in use.
    private static final AtomicLong PHONE = new AtomicLong(2_377_000_000_000L);

    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("a username is still accepted")
    void usernameStillWorks() {
        User_ person = user("named");

        assertThat(login(person.user_name(), "password").getStatusCode())
                .as("every existing account signs in this way, including the seeded admin")
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("an email address is accepted too")
    void emailNowWorks() {
        User_ person = user("mailed");

        assertThat(login(person.email(), "password").getStatusCode())
                .as("this is what the login screen asks for, and it used to be refused")
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("the right email with the wrong password is still refused")
    void emailIsNotAWayPastThePassword() {
        User_ person = user("wrongpass");

        assertThat(login(person.email(), "not-the-password").getStatusCode())
                .as("accepting a second identifier widens what can be named, not what can be "
                        + "skipped - without this the test above would pass on a login that "
                        + "let anybody in")
                .isNotEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("an address nobody signed up with is refused")
    void unknownIdentifierIsRefused() {
        assertThat(login("nobody-at-all@example.test", "password").getStatusCode())
                .isNotEqualTo(HttpStatus.OK);
    }

    /**
     * The one ambiguity worth pinning.
     *
     * Nothing stops a username containing '@', so a username can equal somebody
     * else's email address. Each column is unique on its own, so neither lookup
     * can return two rows - but the pair can disagree about who is signing in,
     * and that has to resolve the same way every time. It resolves to the
     * username, which is the value login has always accepted.
     */
    @Test
    @DisplayName("when a username is somebody else's email, the username wins")
    void usernameWinsTheCollision() {
        long seq = PHONE.incrementAndGet();
        String contested = "contested-" + seq + "@example.test";

        User_ byName = users.save(new User_(null, contested, "owner-" + seq + "@example.test",
                passwordEncoder.encode("password"),
                "+" + seq, null, true, "", null, new Date().getTime(), null,
                0, null, "offline", null, 0.0, null));

        long other = PHONE.incrementAndGet();
        users.save(new User_(null, "other-" + other, contested,
                passwordEncoder.encode("a-different-password"),
                "+" + other, null, true, "", null, new Date().getTime(), null,
                0, null, "offline", null, 0.0, null));

        ResponseEntity<String> response = login(contested, "password");
        assertThat(response.getStatusCode())
                .as("the username holder's password is the one that works")
                .isEqualTo(HttpStatus.OK);

        // Which of the two actually holds the session, rather than merely that
        // somebody does. The passwords differ, so a wrong match would have failed
        // above - this says so in the account's own words.
        HttpHeaders session = new HttpHeaders();
        session.add(HttpHeaders.COOKIE, response.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";")[0]);

        ResponseEntity<String> profile = rest.exchange("/req/profile", HttpMethod.GET,
                new HttpEntity<>(session), String.class);

        assertThat(profile.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(profile.getBody())
                .as("the account whose username it is, not the account whose email it is")
                .contains("\"user_name\":\"" + byName.user_name() + "\"");
    }

    // --- helpers ----------------------------------------------------------

    private User_ user(String prefix) {
        long seq = PHONE.incrementAndGet();
        String name = prefix + "-" + seq;
        return users.save(new User_(null, name, name + "@example.test",
                passwordEncoder.encode("password"),
                "+" + seq, null, true, "", null, new Date().getTime(), null,
                0, null, "offline", null, 0.0, null));
    }

    private ResponseEntity<String> login(String identifier, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        // "username" is Spring Security's field name for whatever identifies the
        // account, not a claim about which column it matches.
        form.add("username", identifier);
        form.add("password", password);
        return rest.postForEntity("/login", new HttpEntity<>(form, headers), String.class);
    }
}
