package com.example.World.Media;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Mints custom tokens with the service-account key the Admin SDK already loads.
 *
 * No new dependency and no network call: the key includes a private key, so the
 * token is signed here rather than by asking Google to sign it. That also means
 * this keeps working when Google is unreachable, which matters because a failure
 * would otherwise stop people posting photos rather than just stopping pushes.
 */
@Component
public class FirebaseUploadTokens implements UploadTokens {

    private static final Logger log = LoggerFactory.getLogger(FirebaseUploadTokens.class);

    /**
     * Injected rather than reached for statically so the bean graph says this
     * depends on Firebase being initialised, and so a test can replace it.
     */
    private final FirebaseApp app;

    public FirebaseUploadTokens(FirebaseApp app) {
        this.app = app;
    }

    @Override
    public String forUser(long userId) {
        try {
            return FirebaseAuth.getInstance(app).createCustomToken(String.valueOf(userId));
        } catch (FirebaseAuthException | IllegalArgumentException | IllegalStateException e) {
            // Not rethrown as a 500: nothing the caller did is wrong, and the app
            // shows the difference between "try again" and "that upload was
            // refused". The cause is ours - a missing or unusable service-account
            // key - so it goes in the log with the id that asked.
            log.error("Could not mint an upload token for user {}", userId, e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Uploads are unavailable right now");
        }
    }
}
