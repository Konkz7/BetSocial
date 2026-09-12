package com.example.World.Observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Puts a request id and the caller on every log line the request produces.
 *
 * Without this, a log is a list of sentences with no way to tell which ones
 * belong together. Two people hitting the same endpoint at the same moment
 * interleave, and the line that explains a failure sits between two lines from
 * somebody else's request. Grouping by id is the difference between reading a
 * log and guessing from one.
 *
 * The id is echoed back on the response as well, so a report of "it failed at
 * about four o'clock" can become one exact request.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LogContextFilter extends OncePerRequestFilter {

    /** The MDC keys. Named here because the log format refers to them by name. */
    public static final String REQUEST_ID = "requestId";
    public static final String USER_ID = "userId";

    /** Sent back on the response, so a user can quote it in a bug report. */
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // Honours an inbound id so that, behind a proxy or from a client that
        // generates one, the same request is one id end to end. Truncated and
        // filtered, because this ends up in log lines and a header is attacker
        // controlled - a newline in it would let somebody forge log entries.
        String inbound = request.getHeader(REQUEST_ID_HEADER);
        String requestId = isUsable(inbound)
                ? inbound.substring(0, Math.min(inbound.length(), 64))
                : UUID.randomUUID().toString().substring(0, 8);

        MDC.put(REQUEST_ID, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        // The session is read rather than the security context, because that is
        // where this application keeps identity - see any controller.
        Object uid = request.getSession(false) == null
                ? null
                : request.getSession(false).getAttribute("userId");
        if (uid != null) {
            MDC.put(USER_ID, String.valueOf(uid));
        }

        try {
            chain.doFilter(request, response);
        } finally {
            // Always. Threads are pooled, so anything left behind here would
            // reappear against an unrelated request later - which is worse than
            // no context at all, because it is wrong rather than missing.
            MDC.remove(REQUEST_ID);
            MDC.remove(USER_ID);
        }
    }

    /** Rejects anything that would corrupt a log line rather than label one. */
    private static boolean isUsable(String header) {
        if (header == null || header.isBlank()) {
            return false;
        }
        return header.chars().allMatch(c -> c == '-' || Character.isLetterOrDigit(c));
    }
}
