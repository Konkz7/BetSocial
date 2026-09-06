package com.example.World.Security;

import org.springframework.security.core.Authentication;

import java.security.Principal;

/**
 * Resolves the authenticated user id for a STOMP session.
 *
 * WebSocket identity previously came from a client-supplied native STOMP header
 * ("userId"), which meant any client could claim to be any user - and, because
 * /ws/** was permitAll, without authenticating at all. The principal here is
 * established by Spring Security during the HTTP handshake from the session
 * cookie, so it cannot be set by the client.
 */
public final class WebSocketPrincipal {

    private WebSocketPrincipal() {
    }

    /** The authenticated user's id, or null when the session has no principal. */
    public static Long userIdOf(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof CustomUserDetails details) {
            return details.getUserId();
        }
        return null;
    }

    /** As {@link #userIdOf}, but rejects an unauthenticated session. */
    public static Long requireUserId(Principal principal) {
        Long uid = userIdOf(principal);
        if (uid == null) {
            throw new IllegalStateException("No authenticated user on this WebSocket session");
        }
        return uid;
    }
}
