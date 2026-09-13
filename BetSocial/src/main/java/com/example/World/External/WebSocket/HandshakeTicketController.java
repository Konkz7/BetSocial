package com.example.World.External.WebSocket;

import com.example.World.RateLimit.Limits;
import com.example.World.RateLimit.RateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Hands the app a ticket to open its socket with.
 *
 * Over HTTP, where the session cookie demonstrably works - that is the whole
 * point of doing it here rather than trying harder to make the handshake carry
 * one. The ticket is for whoever is signed in and nobody else: there is no
 * parameter naming a user, because that would turn a fallback into a way to
 * open somebody else's socket.
 */
@RestController
@RequestMapping("/api/ws")
class HandshakeTicketController {

    private final HandshakeTickets tickets;
    private final RateLimiter rateLimiter;

    HandshakeTicketController(HandshakeTickets tickets, RateLimiter rateLimiter) {
        this.tickets = tickets;
        this.rateLimiter = rateLimiter;
    }

    record Ticket(String ticket, long expiresInSeconds) {
    }

    @PostMapping("/ticket")
    Ticket ticket(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }

        // Generous, because reconnecting spends one each time and a bad network
        // reconnects a lot. Tight enough that a script cannot mint them forever.
        rateLimiter.require(
                RateLimiter.scopeOf("ws-ticket", authentication.getName()), Limits.WS_TICKETS);

        return new Ticket(tickets.issue(authentication.getName()),
                HandshakeTickets.LIFETIME.toSeconds());
    }
}
