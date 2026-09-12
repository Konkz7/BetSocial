package com.example.World.External.Emails;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    /**
     * Where the links in these emails point.
     *
     * Was hardcoded to http://localhost:8080, which is only ever right on the
     * machine running the server. Every verification email sent to a real address
     * contained a link that could not work - and the reset link below would have
     * had the same problem, which matters more, because a verification link that
     * fails is an annoyance and a reset link that fails is a locked-out account.
     */
    private final String baseUrl;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.mailSender = mailSender;
        this.baseUrl = baseUrl;
    }

    public void sendVerificationEmail(String email, String token) {
        send(email, "Verify Your Email",
                "Click the link below to verify your email:\n"
                        + baseUrl + "/req/verify-email?token=" + encode(token));
    }

    /**
     * The link back into a forgotten account.
     *
     * Says how long it lasts, because a link that has quietly expired looks like
     * a broken app rather than an expected thing, and says to ignore it if
     * unexpected, because an unexpected reset email is how somebody finds out
     * their address is being used.
     */
    public void sendPasswordResetEmail(String email, String token) {
        send(email, "Reset your BetSocial password",
                "Somebody asked to reset the password for this address.\n\n"
                        + "Open this link within the next hour to set a new one:\n"
                        + baseUrl + "/req/reset-password?token=" + encode(token) + "\n\n"
                        + "If that was not you, ignore this email. Your password has "
                        + "not changed and nobody has been given access.");
    }

    /**
     * Sends, and does not let a mail failure become the caller's problem.
     *
     * A reset request that returns 500 because the SMTP server is having a
     * moment tells the person their account is broken. The request has already
     * done the part that matters - the token is stored - and the useful answer is
     * "check your email", with the failure logged for whoever can act on it.
     */
    private void send(String to, String subject, String body) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(to);
        mailMessage.setSubject(subject);
        mailMessage.setText(body);

        try {
            mailSender.send(mailMessage);
        } catch (MailException e) {
            log.error("Could not send \"{}\" email", subject, e);
        }
    }

    /** A token in a query string. Tokens are URL-safe already; this is belt. */
    private static String encode(String token) {
        return URLEncoder.encode(token, StandardCharsets.UTF_8);
    }
}
