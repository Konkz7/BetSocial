package com.example.World.Observability;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What a client is allowed to be told.
 *
 * The whole point of this class is the difference between a reason somebody
 * wrote for a person to read and a message a library wrote about itself. The
 * second is the one that leaks, and it leaks exactly when things are going
 * worst - a 500 from the driver naming the host, the database and the query.
 */
@DisplayName("Error responses")
class ApiErrorHandlerTest {

    private final ApiErrorHandler handler = new ApiErrorHandler();

    @Test
    @DisplayName("an unexpected failure tells the client nothing about itself")
    void saysNothingAboutTheCause() {
        // The shape of a real one: a connection failure naming the host, the
        // database, the user and the driver.
        Exception realistic = new IllegalStateException(
                "FATAL: password authentication failed for user \"betsocial\" "
                        + "connecting to jdbc:postgresql://10.0.0.7:5432/betsocial");

        ResponseEntity<Map<String, Object>> response = handler.handleAnythingElse(realistic);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        String body = response.getBody().toString();
        assertThat(body)
                .as("none of this is the caller's to know, and all of it is useful "
                        + "to somebody deciding whether to keep going")
                .doesNotContain("jdbc")
                .doesNotContain("postgresql")
                .doesNotContain("10.0.0.7")
                .doesNotContain("betsocial")
                .doesNotContain("password")
                .doesNotContain("IllegalStateException");

        assertThat(response.getBody().get("message")).isEqualTo("Something went wrong");
    }

    @Test
    @DisplayName("the message field is the one the app reads")
    void keepsTheFieldTheClientLooksAt() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleAnythingElse(new RuntimeException("anything"));

        // API.js reads error.response?.data?.message. Renaming this would turn
        // every error in the app into "Request failed with status code 500".
        assertThat(response.getBody()).containsKey("message");
        assertThat(response.getBody()).containsEntry("status", 500);
    }

    @Test
    @DisplayName("a reason somebody wrote reaches the client intact")
    void passesThroughWrittenReasons() {
        // The exact one StakingTest asserts on. It is written for a person, and
        // the numbers in it are the whole value of the message.
        ResponseStatusException refusal = new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "That would need 1001 coins and you have 1000");

        ResponseEntity<Map<String, Object>> response = handler.handleWrittenReason(refusal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message"))
                .as("reading ProblemDetail.getDetail() instead of getReason() turns "
                        + "this into \"Bad Request\", which is how it first broke")
                .isEqualTo("That would need 1001 coins and you have 1000");
    }

    @Test
    @DisplayName("a written reason survives even on a 5xx")
    void keepsWrittenReasonsOnServerErrors() {
        // FirebaseUploadTokens throws exactly this when it cannot sign a token.
        ResponseStatusException unavailable = new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, "Uploads are unavailable right now");

        ResponseEntity<Map<String, Object>> response = handler.handleWrittenReason(unavailable);

        assertThat(response.getBody().get("message"))
                .as("suppressing by status would lose a deliberate message exactly "
                        + "where the app most needs something to show")
                .isEqualTo("Uploads are unavailable right now");
    }

    @Test
    @DisplayName("a reasonless refusal still says something")
    void fallsBackToTheStatusPhrase() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleWrittenReason(new ResponseStatusException(HttpStatus.NOT_FOUND));

        assertThat(response.getBody().get("message")).isEqualTo("Not Found");
    }

    @Test
    @DisplayName("being refused is not the same as being broken")
    void refusalIsNotAFailure() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleAccessDenied(new AccessDeniedException("Access Denied"));

        assertThat(response.getStatusCode())
                .as("without this the catch-all would answer 500 to an ordinary 403")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
