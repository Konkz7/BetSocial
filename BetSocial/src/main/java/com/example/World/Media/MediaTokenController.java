package com.example.World.Media;

import com.example.World.RateLimit.Limits;
import com.example.World.RateLimit.RateLimiter;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Hands the client an identity to upload with.
 *
 * The only thing standing between a caller and a token is the session, which is
 * the point: the token says "this is user 7", so it must only ever be issued to
 * user 7. The id comes from the session and is never read from the request -
 * a uid parameter here would let anybody mint a token for anybody, and the
 * Storage rules would then be enforcing an identity the caller chose.
 */
@RestController
@RequestMapping("/api/media")
class MediaTokenController {

    private static final Logger log = LoggerFactory.getLogger(MediaTokenController.class);

    private final UploadTokens uploadTokens;
    private final RateLimiter rateLimiter;

    MediaTokenController(UploadTokens uploadTokens, RateLimiter rateLimiter) {
        this.uploadTokens = uploadTokens;
        this.rateLimiter = rateLimiter;
    }

    /** What the client needs to sign in to Firebase, and how long it has to do it. */
    record UploadToken(String token, long expiresInSeconds) {
    }

    @PostMapping("/token")
    UploadToken token(HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        if (uid == null) {
            // Belt and braces - SecurityConfig already requires authentication on
            // anything under /api. Without this the cast above would hand a null
            // to the minter and produce a token for the uid "null".
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }

        rateLimiter.require(RateLimiter.scopeOf("upload-token", uid), Limits.UPLOAD_TOKENS);

        log.debug("Minted an upload token for user {}", uid);
        return new UploadToken(uploadTokens.forUser(uid), UploadTokens.LIFETIME.toSeconds());
    }
}
