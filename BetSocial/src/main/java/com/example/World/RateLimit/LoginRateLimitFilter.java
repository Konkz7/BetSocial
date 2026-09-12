package com.example.World.RateLimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Refuses sign-in attempts from an address that has been guessing.
 *
 * Sits in front of Spring Security's login filter, because the point is to stop
 * the password being checked at all. Counting failures afterwards records the
 * abuse without preventing it - by the time the failure handler runs, whoever is
 * guessing has already learned whether that guess was right.
 *
 * Only the check happens here. The spending happens in the failure handler, so a
 * person who signs in successfully ten times is not treated like somebody who
 * failed ten times. See RateLimiter.wouldAllow.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;

    public LoginRateLimitFilter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    /** The key a sign-in is limited against. Shared with the failure handler. */
    public static String keyFor(HttpServletRequest request) {
        return RateLimiter.scopeOf("login", request.getRemoteAddr());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod())
                && "/login".equals(request.getServletPath()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        if (!rateLimiter.wouldAllow(keyFor(request), Limits.LOGIN)) {
            // Written out rather than taken from HttpServletResponse, which has
            // no constant for it - 429 was standardised after that list was.
            response.setStatus(429);
            response.setContentType("application/json");
            // The same shape the failure handler returns, so a client that only
            // knows how to read one error format still shows something useful.
            response.getWriter().write(
                    "{\"error\": \"Too many sign-in attempts. Try again shortly.\"}");
            response.getWriter().flush();
            return;
        }

        chain.doFilter(request, response);
    }
}
