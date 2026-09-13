package com.example.World.External.WebSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * One-shot permission to open a WebSocket.
 *
 * The handshake needs an authenticated session, and the only way it had of
 * carrying one was the JSESSIONID cookie. React Native's Android WebSocket does
 * attach cookies to a ws:// handshake - WebSocketModule.getCookie reads the same
 * store the HTTP stack writes to, mapping ws:// to http:// so the domain matches
 * - so this was expected to work, and evidently does not do so reliably here.
 * iOS was never verified at all.
 *
 * Rather than keep guessing at somebody else's cookie jar, the app now asks for
 * a ticket over HTTP, where authentication already works, and puts it in the
 * handshake URL. The cookie is still tried first and this only fills in when it
 * did not arrive, so a working cookie costs nothing and spends no ticket.
 *
 * A ticket in a URL is a credential somewhere credentials do not belong: query
 * strings reach logs and history. So it is unguessable, usable once, and dead in
 * thirty seconds - it exists only to survive the gap between asking for it and
 * the socket opening.
 *
 * This is deliberately its own class rather than a generalisation of
 * DownloadTokens, which has the same shape. Two similar things are not yet a
 * pattern, and merging them would put the export - which works - at risk for the
 * sake of tidiness. Worth doing when a third appears.
 */
@Component
public class HandshakeTickets {

    private static final Logger log = LoggerFactory.getLogger(HandshakeTickets.class);

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Long enough for one round trip, and no longer.
     *
     * The client asks for a ticket immediately before connecting, so this only
     * has to cover a request and a handshake on the same network.
     */
    static final Duration LIFETIME = Duration.ofSeconds(30);

    /** Hash of the ticket, to the account it opens a socket for. */
    private final Map<String, Grant> grants = new ConcurrentHashMap<>();

    private record Grant(String username, long expiresAt) {
    }

    /**
     * Issues a ticket for one handshake.
     *
     * Holds the username rather than the id so that consuming it goes back
     * through UserDetailsService - which is where an account being deleted or
     * having its roles changed is noticed. Building an authentication here from
     * a raw id would quietly skip all of that.
     */
    public String issue(String username) {
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        grants.put(hash(ticket), new Grant(username, System.currentTimeMillis() + LIFETIME.toMillis()));
        return ticket;
    }

    /**
     * Spends a ticket and says whose socket it opens.
     *
     * Removed before it is checked, so two handshakes racing cannot both use it.
     * Empty for unknown, already spent or expired - all three are the same
     * answer, because distinguishing them says whether a ticket ever existed.
     */
    public Optional<String> consume(String ticket) {
        Grant grant = ticket == null ? null : grants.remove(hash(ticket));

        if (grant == null || grant.expiresAt() < System.currentTimeMillis()) {
            return Optional.empty();
        }

        return Optional.of(grant.username());
    }

    /** Drops tickets nobody used, which consume would otherwise never remove. */
    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    void evictExpired() {
        long now = System.currentTimeMillis();
        int before = grants.size();
        grants.values().removeIf(grant -> grant.expiresAt() < now);

        int removed = before - grants.size();
        if (removed > 0) {
            log.debug("Evicted {} unused handshake tickets", removed);
        }
    }

    private static String hash(String ticket) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(ticket.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
