package com.example.World.External.WebSocket;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import com.example.World.Users.UserService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Lets a WebSocket handshake authenticate with a ticket when the cookie did not
 * arrive.
 *
 * Only the handshake, and only when there is nothing better: if the session
 * cookie was sent, the request is already authenticated by the time this runs
 * and the ticket is left unspent. So this is a fallback, not a replacement -
 * /ws/** still requires authentication, and the identity the socket ends up with
 * is established the same way it always was.
 */
@Component
public class HandshakeTicketFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HandshakeTicketFilter.class);

    private final HandshakeTickets tickets;

    /**
     * The concrete service, not the UserDetailsService bean.
     *
     * That bean is declared by SecurityConfig, which holds this filter - asking
     * for it by interface makes a cycle the context refuses to start with.
     * UserService is a @Service in its own right, so this is the same object
     * reached without going back through the configuration that needs us.
     */
    private final UserService userService;

    public HandshakeTicketFilter(HandshakeTickets tickets, UserService userService) {
        this.tickets = tickets;
        this.userService = userService;
    }

    /**
     * The handshake only.
     *
     * A ticket is a credential in a query string, which is a thing to keep as
     * small as possible: it should open a socket and be useless everywhere else.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.equals("/ws") || path.startsWith("/ws/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!alreadyAuthenticated() && request.getParameter("ticket") != null) {
            authenticateFromTicket(request.getParameter("ticket"));
        }

        chain.doFilter(request, response);
    }

    private void authenticateFromTicket(String ticket) {
        Optional<String> username = tickets.consume(ticket);
        if (username.isEmpty()) {
            // Unknown, spent or expired. Nothing is set, so the authorisation
            // rule refuses the handshake exactly as it would have anyway.
            log.debug("A handshake presented a ticket that was not valid");
            return;
        }

        try {
            // Through UserDetailsService rather than straight from the ticket:
            // this is where a deleted account is refused and where the current
            // roles come from. A ticket issued a moment ago must not outlive
            // either of those.
            UserDetails details = userService.loadUserByUsername(username.get());

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new UsernamePasswordAuthenticationToken(
                    details, null, details.getAuthorities()));
            SecurityContextHolder.setContext(context);

            log.debug("Authenticated a WebSocket handshake from a ticket");
        } catch (UsernameNotFoundException gone) {
            log.debug("A handshake ticket named an account that can no longer sign in");
        }
    }

    private static boolean alreadyAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
