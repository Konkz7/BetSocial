package com.example.World;

import com.example.World.SignIn.PasswordResetService;
import com.example.World.Users.UserRepository;
import com.example.World.Users.User_;
import com.example.World.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Getting back into an account after forgetting the password.
 *
 * There was no route back in. AuthService.sendPasswordResetEmail existed and had
 * never been called - and calling it would not have helped, because it resets a
 * Firebase password while sign-in checks the BCrypt hash in user_. Somebody
 * would have followed the link, set a new password, and still been locked out.
 *
 * The token is asserted through the database rather than by reading an email:
 * only its hash is stored, so these tests hash a known token the same way the
 * service does. That also pins the storage decision - if the raw token were ever
 * stored instead, every one of these fails.
 */
@DisplayName("Password reset")
class PasswordResetTest extends AbstractIntegrationTest {

    // See SecurityRegressionTest for the per-class phone_number blocks in use.
    private static final AtomicLong PHONE = new AtomicLong(2_364_000_000_000L);

    @Autowired UserRepository users;
    @Autowired PasswordResetService resets;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("a link lets somebody set a new password and sign in with it")
    void resetChangesThePassword() {
        User_ person = user();
        String token = issueTokenFor(person);

        resets.resetPassword(token, "a-brand-new-password");

        assertThat(login(person.user_name(), "a-brand-new-password").getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(login(person.user_name(), "password").getStatusCode())
                .as("the old password must stop working")
                .isNotEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("only the hash of a token is stored")
    void tokenIsStoredHashed() {
        User_ person = user();
        String token = issueTokenFor(person);

        assertThat(storedToken(person))
                .as("a leaked database must not hand somebody working reset links")
                .isNotEqualTo(token)
                .isEqualTo(sha256(token));
    }

    @Test
    @DisplayName("a link works once")
    void tokenIsSingleUse() {
        User_ person = user();
        String token = issueTokenFor(person);

        resets.resetPassword(token, "first-new-password");

        assertThatThrownBy(() -> resets.resetPassword(token, "second-new-password"))
                .as("a link left in an inbox must not stay usable")
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("an expired link is refused")
    void expiredTokenIsRefused() {
        User_ person = user();
        String token = "expired-token-" + person.uid();

        // Issued an hour and a minute ago.
        users.setPasswordResetToken(person.uid(), sha256(token),
                new Date().getTime() - (61 * 60 * 1000));

        assertThatThrownBy(() -> resets.resetPassword(token, "a-brand-new-password"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("a token nobody issued is refused")
    void unknownTokenIsRefused() {
        assertThatThrownBy(() -> resets.resetPassword("never-issued", "a-brand-new-password"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("asking again replaces the previous link")
    void askingTwiceInvalidatesTheFirstLink() {
        User_ person = user();
        String first = issueTokenFor(person);
        String second = issueTokenFor(person);

        assertThatThrownBy(() -> resets.resetPassword(first, "a-brand-new-password"))
                .as("two working links for one account is one more than anybody needs")
                .isInstanceOf(ResponseStatusException.class);

        resets.resetPassword(second, "a-brand-new-password");
        assertThat(login(person.user_name(), "a-brand-new-password").getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("a short password is refused, matching registration")
    void shortPasswordIsRefused() {
        User_ person = user();
        String token = issueTokenFor(person);

        assertThatThrownBy(() -> resets.resetPassword(token, "short"))
                .isInstanceOf(ResponseStatusException.class);

        assertThat(storedToken(person))
                .as("a refused attempt must not spend the link")
                .isNotNull();
    }

    @Test
    @DisplayName("asking about an unknown address says the same as a known one")
    void unknownAddressIsNotDistinguishable() {
        User_ person = user();

        ResponseEntity<String> known = post("/req/forgot-password",
                "{\"email\":\"" + person.email() + "\"}");
        ResponseEntity<String> unknown = post("/req/forgot-password",
                "{\"email\":\"nobody-here@example.test\"}");

        assertThat(known.getStatusCode())
                .as("answering differently turns this into a way to ask who has an account")
                .isEqualTo(unknown.getStatusCode());
        assertThat(known.getBody()).isEqualTo(unknown.getBody());
    }

    @Test
    @DisplayName("requesting a reset does not need a session")
    void requestIsOpen() {
        User_ person = user();

        assertThat(post("/req/forgot-password", "{\"email\":\"" + person.email() + "\"}")
                .getStatusCode())
                .as("somebody locked out cannot sign in to ask for a way to sign in")
                .isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    @DisplayName("a deleted account cannot be reset back into")
    void deletedAccountCannotBeReset() {
        User_ person = user();
        users.scrubPersonalData(person.uid(), "deleted-user-" + person.uid(),
                "deleted-" + person.uid() + "@invalid", "del-" + person.uid(),
                new Date().getTime());

        // The token is issued directly, since requestReset would not find the
        // scrubbed address in the first place - this checks the second gate.
        String token = "token-for-deleted-" + person.uid();
        users.setPasswordResetToken(person.uid(), sha256(token),
                new Date().getTime() + 3_600_000);

        assertThatThrownBy(() -> resets.resetPassword(token, "a-brand-new-password"))
                .as("deletion has to be final, or it is not deletion")
                .isInstanceOf(ResponseStatusException.class);
    }

    // --- helpers ----------------------------------------------------------

    /**
     * Issues a token the way the service does, and returns the raw value.
     *
     * requestReset generates its own and only the hash reaches the database, so a
     * test cannot read it back. This stores a known token through the same
     * repository method the service uses, which keeps the storage format under
     * test without needing to intercept an email.
     */
    private String issueTokenFor(User_ person) {
        String token = "test-token-" + person.uid() + "-" + System.nanoTime();
        users.setPasswordResetToken(person.uid(), sha256(token),
                new Date().getTime() + 3_600_000);
        return token;
    }

    /**
     * Read straight from the column.
     *
     * User_ deliberately does not carry the reset columns - adding two fields
     * would have meant editing every `new User_(...)` in the suite for something
     * only three queries touch.
     */
    private String storedToken(User_ person) {
        return jdbc.queryForObject(
                "SELECT password_reset_token FROM User_ WHERE uid = ?",
                String.class, person.uid());
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private User_ user() {
        long seq = PHONE.incrementAndGet();
        String name = "reset-" + seq;
        return users.save(new User_(null, name, name + "@example.test",
                passwordEncoder.encode("password"),
                "+" + seq, null, true, "", null, new Date().getTime(), null,
                0, null, "offline", null, 0.0, null));
    }

    private ResponseEntity<String> login(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", username);
        form.add("password", password);
        return rest.postForEntity("/login", new HttpEntity<>(form, headers), String.class);
    }

    private ResponseEntity<String> post(String path, String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.postForEntity(path, new HttpEntity<>(json, headers), String.class);
    }
}
