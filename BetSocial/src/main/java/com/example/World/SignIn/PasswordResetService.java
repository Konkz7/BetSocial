package com.example.World.SignIn;

import com.example.World.External.Emails.EmailService;
import com.example.World.Users.UserRepository;
import com.example.World.Users.User_;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Getting back into an account after forgetting the password.
 *
 * There was no route back in at all. AuthService.sendPasswordResetEmail existed
 * and had never been called - and calling it would not have worked, because it
 * asks Firebase Auth to reset a Firebase password while sign-in goes through
 * DaoAuthenticationProvider against the BCrypt hash in user_. Somebody would
 * have followed the link, set a new password, and still been locked out.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    /**
     * How long a link lasts.
     *
     * An hour is long enough to find the email and short enough that a link left
     * sitting in an inbox stops being a second password before long.
     */
    private static final Duration VALID_FOR = Duration.ofHours(1);

    /** Matches the check in AccountController.checkDetails. */
    private static final int MIN_PASSWORD_LENGTH = 8;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(UserRepository userRepository, EmailService emailService,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Sends a reset link, if that address belongs to an account.
     *
     * Returns nothing and says nothing either way. Answering "no such account"
     * turns this endpoint into a way to ask whether somebody has one, which for a
     * social app is worth more to whoever is asking than it is to the person who
     * mistyped their address.
     */
    public void requestReset(String email) {
        Optional<User_> account = userRepository.findByEmail(email);

        if (account.isEmpty()) {
            // Logged, because a lot of these for addresses that do not exist is
            // worth being able to see.
            log.info("Password reset asked for an address with no account");
            return;
        }

        User_ user = account.get();

        // The token goes in the email; only its hash is stored. A leaked database
        // should not hand somebody a working link for every account mid-reset.
        String token = newToken();
        long expiresAt = new Date().getTime() + VALID_FOR.toMillis();

        userRepository.setPasswordResetToken(user.uid(), hash(token), expiresAt);
        emailService.sendPasswordResetEmail(user.email(), token);

        log.info("Password reset link issued for user {}", user.uid());
    }

    /**
     * Sets a new password, given a link that has not expired or been used.
     *
     * Transactional and public so the annotation takes effect: the password and
     * the token are cleared in one statement, but the length check and the lookup
     * happen first and should not be able to interleave with another attempt.
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A password needs at least " + MIN_PASSWORD_LENGTH + " characters");
        }

        String hashed = hash(token);
        User_ user = userRepository
                .findByValidPasswordResetToken(hashed, new Date().getTime())
                // One answer for expired, already used, and never existed. Which
                // of the three it was is not information the person asking needs,
                // and it is information somebody guessing would like.
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "That reset link is no longer valid. Ask for a new one."));

        int updated = userRepository.applyPasswordReset(
                user.uid(), hashed, passwordEncoder.encode(newPassword));

        if (updated == 0) {
            // Another request used the same link between the lookup and here.
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "That reset link has already been used.");
        }

        log.info("Password reset completed for user {}", user.uid());
    }

    /**
     * 32 random bytes, URL-safe.
     *
     * Long enough that guessing is not a strategy, and URL-safe because it goes
     * in a link - a token needing escaping is a token that arrives corrupted.
     */
    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256, not BCrypt.
     *
     * BCrypt is right for passwords because it is deliberately slow against
     * guessing a weak secret. This secret is 32 random bytes, so guessing is not
     * the threat - and BCrypt's per-hash salt would make the token impossible to
     * look up by index, which is the whole point of storing it.
     */
    private static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            // Every JVM ships SHA-256; this cannot happen.
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
