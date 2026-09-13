package com.example.World.Observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Whether this instance should be sent traffic.
 *
 * A host needs somewhere to ask that, and needs it to be honest: an application
 * that answers 200 while it cannot reach its database gets a rolling deploy that
 * replaces working instances with broken ones and reports success.
 *
 * So this touches the database rather than just proving the process is alive.
 * The cost is that a database blip takes instances out of rotation - which is
 * the right answer, because an instance that cannot read anything has nothing to
 * serve.
 *
 * Hand-written rather than Actuator. Actuator is the usual answer and would also
 * bring metrics and liveness/readiness groups, but it is a dependency and a set
 * of endpoints to secure for one thing this needs today. Worth revisiting the
 * day something wants metrics.
 */
@RestController
class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final JdbcTemplate jdbc;

    HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/health")
    ResponseEntity<Map<String, String>> health() {
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (Exception unreachable) {
            // At warn rather than error, and without the stack trace: a host polls
            // this every few seconds, so an outage would otherwise write the same
            // trace hundreds of times and bury whatever else was happening.
            log.warn("Health check failed: the database is not reachable ({})",
                    unreachable.getClass().getSimpleName());

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("status", "database unreachable"));
        }
    }
}
