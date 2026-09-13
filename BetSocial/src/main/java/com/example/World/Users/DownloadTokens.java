package com.example.World.Users;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One-shot permission to download your own data.
 *
 * The export has to arrive as a file, and the only thing on a phone that saves a
 * file where its owner can find it afterwards is the browser's own download
 * handling. The browser is not the app, so it does not carry the app's session
 * cookie - it needs something in the URL instead.
 *
 * That is worth being careful about. A URL ends up in browser history and is the
 * sort of thing people paste into messages, and this one fetches somebody's
 * email address, phone number and private messages. So: unguessable, usable
 * once, and dead two minutes after it is issued. By the time such a link could
 * be found anywhere it has already stopped working.
 *
 * Kept in memory. These live for two minutes, so losing them on restart costs
 * somebody one tap, and a table would have to be swept. That does assume one
 * server - with two, the browser could be handed to the one that did not issue
 * the token. This class is where that gets fixed when it happens.
 */
@Component
public class DownloadTokens {

    private static final Logger log = LoggerFactory.getLogger(DownloadTokens.class);

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Long enough to open a browser and no longer.
     *
     * The window is the whole security story here, so it is as short as the
     * slowest plausible hand-off allows rather than as long as is convenient.
     */
    static final Duration LIFETIME = Duration.ofMinutes(2);

    /** Hash of the token, to what it grants. */
    private final Map<String, Grant> grants = new ConcurrentHashMap<>();

    private record Grant(long userId, long expiresAt) {
    }

    /**
     * Issues a token for one download of this user's data.
     *
     * @return the token itself, which is not stored - only its hash is, so a
     *         heap dump or a log line cannot be turned back into a working link.
     */
    public String issue(long userId) {
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        grants.put(hash(token), new Grant(userId, System.currentTimeMillis() + LIFETIME.toMillis()));
        return token;
    }

    /**
     * Spends a token and says whose data it was for.
     *
     * Removed before it is checked, so a token cannot be used twice even by two
     * requests arriving at once - which is not a theoretical concern, because
     * some browsers fetch a URL twice.
     *
     * @throws ResponseStatusException if the token is unknown, already used or
     *         past its two minutes. All three are the same answer on purpose:
     *         telling the difference would say whether a token had ever existed.
     */
    public long consume(String token) {
        Grant grant = token == null ? null : grants.remove(hash(token));

        if (grant == null || grant.expiresAt() < System.currentTimeMillis()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "That download link is no longer valid");
        }

        return grant.userId();
    }

    /**
     * Drops tokens nobody used.
     *
     * Without this the map only ever grows: consume removes the ones that are
     * spent, and a link somebody never opened would otherwise sit there for the
     * life of the process.
     */
    @Scheduled(fixedRate = 10, timeUnit = java.util.concurrent.TimeUnit.MINUTES)
    void evictExpired() {
        long now = System.currentTimeMillis();
        int before = grants.size();
        grants.values().removeIf(grant -> grant.expiresAt() < now);

        int removed = before - grants.size();
        if (removed > 0) {
            log.debug("Evicted {} unused download tokens", removed);
        }
    }

    private static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            // SHA-256 is required of every JVM.
            throw new IllegalStateException(impossible);
        }
    }
}
