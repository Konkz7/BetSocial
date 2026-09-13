package com.example.World.Observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * What a failed request says back.
 *
 * There are two kinds of message and they were being treated as one. A reason on
 * a ResponseStatusException was written by whoever threw it, for the person who
 * will read it - "You cannot stake more than you have" - and the app shows it. A
 * message on an arbitrary exception was written by a library, about itself, and
 * on a 500 it is usually a driver describing the database.
 *
 * server.error.include-message decides between them with one switch, so the
 * choice was either to leak the second or lose the first. Development kept both
 * and production would have lost both, which is the wrong trade in each place:
 * a deployed server should still be able to tell somebody why their stake was
 * refused.
 *
 * So the distinction is made here instead of by configuration. Author-written
 * reasons are passed through, everything else becomes "Something went wrong" and
 * goes to the log with the request id that produced it.
 *
 * Extending ResponseEntityExceptionHandler rather than only catching Exception:
 * it already maps every standard Spring MVC failure - a wrong method, malformed
 * JSON, a missing parameter - to its proper status. Catching Exception alone
 * would turn all of those into 500s.
 */
@RestControllerAdvice
class ApiErrorHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiErrorHandler.class);

    /**
     * What a client is told when the cause is not theirs to know.
     *
     * Deliberately the same sentence every time. Different wording per failure
     * would describe the inside of the server to somebody probing it.
     */
    private static final String GENERIC = "Something went wrong";

    /**
     * Every standard Spring MVC failure, reshaped but not reinterpreted.
     *
     * The status is whatever the framework decided, which is right; only the body
     * changes, so the app sees one shape everywhere. The detail on these is
     * framework text about the request ("Request method 'PUT' is not supported"),
     * which is about what the caller sent rather than about this server.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        String message = body instanceof ProblemDetail problem && problem.getDetail() != null
                ? problem.getDetail()
                : reasonPhraseOf(status);

        if (status.is5xxServerError()) {
            log.error("Request failed with {}", status, exception);
            message = GENERIC;
        }

        return new ResponseEntity<>(payload(status, message), headers, status);
    }

    /**
     * Validation failures, with the field that failed.
     *
     * The messages come from the annotations on the record being validated, which
     * are ours - "must not be empty", "size must be between 0 and 50". Naming the
     * field is the difference between a form that can point at the problem and
     * one that says only that something is wrong.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        if (message.isBlank()) {
            message = "That request was not valid";
        }

        return new ResponseEntity<>(payload(status, message), headers, status);
    }

    /**
     * The reasons this application writes for itself.
     *
     * Handled apart from everything else because ResponseStatusException keeps
     * its reason in getReason() and leaves the ProblemDetail's detail null - so
     * reading the detail, which is what the generic path does, turns "That would
     * need 1001 coins and you have 1000" into "Bad Request".
     *
     * The reason passes through whatever the status is, including 5xx. Every one
     * of these was typed by somebody in this codebase for a person to read - the
     * 503 when upload tokens cannot be minted is as deliberate as the 400 above -
     * and suppressing them by status would lose the message exactly where the
     * app most needs something to show.
     */
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, Object>> handleWrittenReason(ResponseStatusException refusal) {
        HttpStatusCode status = refusal.getStatusCode();

        if (status.is5xxServerError()) {
            // Still worth the log. The client is being told something useful, but
            // a 5xx is ours to explain either way.
            log.error("Request failed with {}", status, refusal);
        }

        String message = refusal.getReason() != null && !refusal.getReason().isBlank()
                ? refusal.getReason()
                : reasonPhraseOf(status);

        return ResponseEntity.status(status).body(payload(status, message));
    }

    /**
     * Refused rather than broken.
     *
     * Only reachable when something inside a controller throws this; the filter
     * chain handles its own. Without it the catch-all below would answer 500 to
     * what is a perfectly ordinary 403.
     */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException denied) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(payload(HttpStatus.FORBIDDEN, "You do not have access to that"));
    }

    /**
     * Anything not accounted for above.
     *
     * The exception goes to the log in full and nothing of it goes to the client.
     * The request id does go back, so "it failed around four o'clock" can become
     * one exact request - it is already on the response as X-Request-Id, and
     * repeating it in the body means the app can show it without reading headers.
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> handleAnythingElse(Exception unexpected) {
        log.error("Unhandled exception", unexpected);

        Map<String, Object> body = payload(HttpStatus.INTERNAL_SERVER_ERROR, GENERIC);
        String requestId = MDC.get("requestId");
        if (requestId != null && !requestId.isBlank()) {
            body.put("requestId", requestId);
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    /**
     * One shape for every failure.
     *
     * "message" because that is the field the app reads - changing it would make
     * every error in the client read "Request failed with status code 400".
     */
    private static Map<String, Object> payload(HttpStatusCode status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", reasonPhraseOf(status));
        body.put("message", message);
        return body;
    }

    private static String reasonPhraseOf(HttpStatusCode status) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        return resolved == null ? "Error" : resolved.getReasonPhrase();
    }
}
