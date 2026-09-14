package com.example.World;

import com.example.World.External.Emails.EmailService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.example.World.Users.UserRole.USER;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Registering with a username, email or phone number somebody already has.
 *
 * All three are UNIQUE in V1__baseline_schema.sql, and until now nothing in
 * /req/register looked at that - the insert simply failed. The failure does not
 * arrive where a catch would expect it either: Spring Data JDBC's executor
 * catches the translated DataIntegrityViolationException and rethrows it inside
 * DbActionExecutionException, which extends RuntimeException rather than
 * DataAccessException, so it fell through to ApiErrorHandler's catch-all. The
 * person retyping an address they had used last month got "Something went
 * wrong" and a 500.
 *
 * Two guards answer these, and the difference between them is the point of the
 * class. register pre-checks the three details, which is what names the field
 * for the first three tests. The soft-delete case defeats that pre-check: none
 * of the three constraints carry a deleted_at predicate and every exists* query
 * in UserRepository does, so a suspended account holds its email while
 * answering "no" to being asked whether that email is taken. Only the insert
 * knows, which is why the pre-check is not the whole of it.
 */
@DisplayName("Registering with details somebody already has")
class DuplicateRegistrationTest extends AbstractIntegrationTest {

    // See SecurityRegressionTest for the per-class phone_number blocks in use.
    // 2_378 is RegistrationEmailTest's; phone_number is unique across the shared
    // database, so a borrowed block fails whichever class runs second.
    private static final AtomicLong PHONE = new AtomicLong(2_379_000_000_000L);

    /**
     * Replaced for the context rather than for the mail.
     *
     * Every registration here is refused before the send, so nothing would go
     * out either way. What this buys is a separate application context: the
     * limiter is in-process, Limits.REGISTER allows five an hour per address and
     * every request in the suite comes from 127.0.0.1, so sharing a context with
     * another class that registers would mean sharing one allowance with it. The
     * four tests below spend four of the five - a fifth needs the limit dealt
     * with, not another POST.
     */
    @MockitoBean EmailService emailService;

    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("an email somebody else has is refused, and says so")
    void duplicateEmailIsRefused() {
        User_ existing = user();

        ResponseEntity<String> response = register(
                fresh("name"), existing.email(), fresh("+"));

        assertThat(response.getStatusCode())
                .as("a detail that is taken is the person's to fix, not a server fault")
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .as("which of the three it was is the whole of the useful information")
                .isEqualTo("Email is already in use.");
        assertNothingLeaked(response.getBody());

        assertThat(rowsWithEmail(existing.email()))
                .as("the refused registration should leave nothing behind")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("a username somebody else has is refused, and says so")
    void duplicateUsernameIsRefused() {
        User_ existing = user();

        ResponseEntity<String> response = register(
                existing.user_name(), fresh("dup") + "@example.test", fresh("+"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .as("naming the email here would send them to change the wrong field")
                .isEqualTo("Username is already in use.");
        assertNothingLeaked(response.getBody());
    }

    @Test
    @DisplayName("a phone number somebody else has is refused, and says so")
    void duplicatePhoneNumberIsRefused() {
        User_ existing = user();

        ResponseEntity<String> response = register(
                fresh("name"), fresh("dup") + "@example.test", existing.phone_number());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Phone number is already in use.");
        assertNothingLeaked(response.getBody());
    }

    /**
     * The case a pre-check cannot answer.
     *
     * suspend() sets deleted_at and touches nothing else, so the row still holds
     * the email while existsByEmail - which filters deleted_at IS NULL - says
     * the address is free. Only the insert knows, which is why the guard is
     * where it is. Before it, this was the 500.
     */
    @Test
    @DisplayName("a suspended account still holds its email, and the answer says so")
    void softDeletedAccountStillHoldsItsDetails() {
        User_ suspended = user();
        users.suspend(suspended.uid(), new Date().getTime());

        assertThat(users.existsByEmail(suspended.email()))
                .as("the premise: every exists* query filters deleted_at, the constraints do not")
                .isFalse();

        ResponseEntity<String> response = register(
                fresh("name"), suspended.email(), fresh("+"));

        assertThat(response.getStatusCode())
                .as("the address is genuinely unavailable, however the row got that way")
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Email is already in use.");
        assertNothingLeaked(response.getBody());
    }

    // --- helpers ----------------------------------------------------------

    /**
     * That the answer describes the person's details and not the database.
     *
     * The untranslated failure carried the whole INSERT statement, the
     * constraint name and the colliding value. Any of it in a response is a
     * description of the schema handed to whoever asked for it.
     */
    private static void assertNothingLeaked(String body) {
        assertThat(body.toLowerCase(Locale.ROOT))
                .as("the driver's account of what went wrong belongs in the log")
                .doesNotContain("insert", "user__", "duplicate key", "constraint",
                        "sql", "org.postgresql", "detail: key");
    }

    private ResponseEntity<String> register(String username, String email, String phone) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return rest.postForEntity("/req/register",
                new HttpEntity<>(Map.of(
                        "user_name", username,
                        "pass_word", "a-long-enough-password",
                        "email", email,
                        "phone_number", phone), headers),
                String.class);
    }

    /**
     * An account to collide with, written straight through the repository.
     *
     * Not registered through the endpoint: that would spend a second token of
     * the allowance described above for every test, and what is under test is
     * the response to the second attempt rather than the success of the first.
     */
    private User_ user() {
        String name = fresh("taken");
        return users.save(new User_(
                null, name, name + "@example.test", passwordEncoder.encode("password"),
                fresh("+"), null, true, "", null, new Date().getTime(), null,
                USER.toInt(), null, "offline", null, 0.0, null));
    }

    /** A value nothing else in the suite has, in this class's block. */
    private static String fresh(String prefix) {
        return prefix + PHONE.incrementAndGet();
    }

    private int rowsWithEmail(String email) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM User_ WHERE email = ?", Integer.class, email);
    }
}
