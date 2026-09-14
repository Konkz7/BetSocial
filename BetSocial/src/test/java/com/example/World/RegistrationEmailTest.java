package com.example.World;

import com.example.World.Wallet.LedgerService;
import com.example.World.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

/**
 * Registering when the mail server will not play.
 *
 * The verification email used to be sent before the account was saved, which
 * cost something whichever way it failed: the link carried a token for a row
 * that did not exist yet, and anything thrown by the send came back to the app
 * as "Failed. Please try again." over a registration nothing had refused.
 * Trying again then hits user__email_key and fails for real, so a wrong
 * MAIL_PASSWORD locked people out of an account they could not tell they had.
 *
 * The account is the half worth keeping. is_verified gates nothing today - sign
 * in works without it - so an email that never arrived leaves a usable account
 * with its token still on the row, which a resend can pick up later. An account
 * that was never written has nothing to recover.
 *
 * The mail sender is replaced rather than EmailService, so these run through the
 * real send path and pin where a failure is absorbed as well as when it happens.
 */
@DisplayName("Registration, when the email fails")
class RegistrationEmailTest extends AbstractIntegrationTest {

    // See SecurityRegressionTest for the per-class phone_number blocks in use.
    // 2_377 was free when this was written and had been taken by
    // LoginIdentifierTest by the time it was rebased; phone_number is unique, so
    // a shared block is a constraint violation in whichever class runs second.
    private static final AtomicLong PHONE = new AtomicLong(2_378_000_000_000L);

    @MockitoBean JavaMailSender mailSender;

    @Autowired LedgerService ledger;

    @Test
    @DisplayName("an SMTP failure still leaves the person with an account")
    void smtpFailureKeepsTheAccount() {
        doThrow(new MailSendException("Mail server connection failed"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        Registration attempt = register();

        assertThat(attempt.response().getStatusCode())
                .as("nothing the person did was wrong, so nothing should look like it was")
                .isEqualTo(HttpStatus.OK);
        assertThat(uidOf(attempt.email()))
                .as("an account that was never created cannot be recovered from")
                .isNotNull();
        assertThat(ledger.balanceOf(uidOf(attempt.email())))
                .as("the opening grant belongs to having an account, not to the email")
                .isEqualTo(LedgerService.OPENING_GRANT);
        assertThat(verificationTokenOf(attempt.email()))
                .as("a resend has nothing to send without the token still on the row")
                .isNotNull();
    }

    /**
     * The same failure, thrown as something EmailService does not catch.
     *
     * Its try block covers MailException only, so the test above passes with the
     * send in either position - it never reaches the controller to matter. This
     * one does reach it, which leaves the order as the only thing keeping the
     * account.
     */
    @Test
    @DisplayName("a mail failure that reaches the controller still leaves the account")
    void unexpectedMailFailureKeepsTheAccount() {
        doThrow(new IllegalStateException("Mail server host not specified"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        Registration attempt = register();

        assertThat(uidOf(attempt.email()))
                .as("whatever the send does, it does it to an account that already exists")
                .isNotNull();
    }

    @Test
    @DisplayName("the account is saved by the time the email goes out")
    void accountExistsBeforeTheSend() {
        AtomicBoolean accountExisted = new AtomicBoolean();
        doAnswer(send -> {
            SimpleMailMessage message = send.getArgument(0);
            accountExisted.set(uidOf(message.getTo()[0]) != null);
            return null;
        }).when(mailSender).send(any(SimpleMailMessage.class));

        Registration attempt = register();

        assertThat(accountExisted)
                .as("a verification link for a row that does not exist yet is a link for nothing")
                .isTrue();
        assertThat(uidOf(attempt.email())).isNotNull();
    }

    // --- helpers ----------------------------------------------------------

    private record Registration(String email, ResponseEntity<String> response) {}

    /**
     * One registration through the endpoint.
     *
     * Limits.REGISTER allows five an hour keyed on the caller's address, which is
     * 127.0.0.1 for every test in this class - so three of these is the budget,
     * and a fourth test needs the limit looking at rather than another POST.
     */
    private Registration register() {
        long seq = PHONE.incrementAndGet();
        String email = "register-" + seq + "@example.test";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new Registration(email, rest.postForEntity("/req/register",
                new HttpEntity<>(Map.of(
                        "user_name", "register-" + seq,
                        "pass_word", "a-long-enough-password",
                        "email", email,
                        "phone_number", "+" + seq), headers),
                String.class));
    }

    /** The account, or null if registration did not leave one. */
    private Long uidOf(String email) {
        List<Long> found = jdbc.queryForList(
                "SELECT uid FROM User_ WHERE email = ?", Long.class, email);
        return found.isEmpty() ? null : found.getFirst();
    }

    private String verificationTokenOf(String email) {
        return jdbc.queryForObject(
                "SELECT verification_token FROM User_ WHERE email = ?", String.class, email);
    }
}
