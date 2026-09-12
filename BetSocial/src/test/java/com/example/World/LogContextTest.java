package com.example.World;

import com.example.World.Observability.LogContextFilter;
import com.example.World.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every response carries the id its log lines were tagged with.
 *
 * The id is what turns "it failed at about four o'clock" into one exact request,
 * so it has to reach the caller and not only the log.
 */
@DisplayName("Log context")
class LogContextTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("a request id comes back on the response")
    void requestIdIsReturned() {
        ResponseEntity<String> response = get("/api/threads/active", null);

        assertThat(response.getHeaders().getFirst(LogContextFilter.REQUEST_ID_HEADER))
                .as("an id nobody can see is an id nobody can quote")
                .isNotBlank();
    }

    @Test
    @DisplayName("two requests get different ids")
    void idsAreNotReused() {
        String first = get("/api/threads/active", null)
                .getHeaders().getFirst(LogContextFilter.REQUEST_ID_HEADER);
        String second = get("/api/threads/active", null)
                .getHeaders().getFirst(LogContextFilter.REQUEST_ID_HEADER);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("an id supplied by the caller is kept")
    void inboundIdIsHonoured() {
        ResponseEntity<String> response = get("/api/threads/active", "abc123");

        assertThat(response.getHeaders().getFirst(LogContextFilter.REQUEST_ID_HEADER))
                .as("behind a proxy, one request should be one id end to end")
                .isEqualTo("abc123");
    }

    @Test
    @DisplayName("a header that would corrupt a log line is not kept")
    void inboundIdIsSanitised() {
        // A newline here would let a caller forge log entries: everything after
        // it reads as a separate line, with whatever level and message they like.
        String forged = "abc\ndef INFO nothing to see here";

        String returned = get("/api/threads/active", forged)
                .getHeaders().getFirst(LogContextFilter.REQUEST_ID_HEADER);

        assertThat(returned).isNotEqualTo(forged);
        assertThat(returned).isNotBlank();
    }

    private ResponseEntity<String> get(String path, String requestId) {
        HttpHeaders headers = new HttpHeaders();
        if (requestId != null) {
            headers.add(LogContextFilter.REQUEST_ID_HEADER, requestId);
        }
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }
}
