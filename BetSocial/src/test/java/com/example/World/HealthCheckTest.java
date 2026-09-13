package com.example.World;

import com.example.World.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The endpoint the host routes on.
 *
 * Two things have to be true and neither is obvious from reading the class. It
 * has to answer without a session, or the host polls it, gets a 401 and decides
 * every instance is unhealthy - which on a rolling deploy means refusing to
 * finish one. And it has to say nothing beyond reachability, because it is the
 * one endpoint on the server that anybody can reach.
 */
@DisplayName("Health check")
class HealthCheckTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("answers without a session")
    void isReachableWithoutSigningIn() {
        ResponseEntity<String> response = rest.getForEntity("/health", String.class);

        assertThat(response.getStatusCode())
                .as("everything else on this server requires authentication; a host "
                        + "polling this has none, and a 401 here reads as 'unhealthy'")
                .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody()).contains("ok");
    }

    @Test
    @DisplayName("says nothing about the server beyond whether it is up")
    void leaksNothing() {
        String body = rest.getForEntity("/health", String.class).getBody();

        // Anybody on the internet can read this. Versions, hostnames and database
        // names are what somebody deciding whether to bother with a target wants.
        assertThat(body)
                .doesNotContain("jdbc")
                .doesNotContain("postgres")
                .doesNotContain("password")
                .doesNotContain("0.0.1-SNAPSHOT");

        assertThat(body.length())
                .as("a health response with room for detail will grow some")
                .isLessThan(100);
    }
}
