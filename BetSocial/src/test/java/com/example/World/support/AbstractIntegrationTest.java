package com.example.World.support;

import com.google.firebase.FirebaseApp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests.
 *
 * Starts one PostgreSQL container for the whole suite and points the
 * application at it. Flyway then builds the schema from the real migrations, so
 * these tests exercise the same DDL and native SQL that production runs - which
 * is why a container is used rather than H2: the baseline is pg_dump output and
 * the repositories are native Postgres SQL.
 *
 * The container is static and started once. Testcontainers' Ryuk sidecar
 * removes it when the JVM exits, and sharing it across classes avoids paying
 * container startup per test class.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("betsocial_test")
                    .withUsername("test")
                    .withPassword("test");

    static {
        POSTGRES.start();
    }

    /**
     * Firebase needs a real service-account key to initialise, which has no place
     * in a test run. Replacing the bean stops FirebaseConfig's factory method
     * from being invoked at all. NotificationService already catches failures
     * from FirebaseMessaging, so push simply no-ops here.
     */
    @MockitoBean
    protected FirebaseApp firebaseApp;

    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        // A fresh container has no schema, so V1 must actually run - the opposite
        // of the production setting. This puts the migrations themselves under test.
        registry.add("spring.flyway.baseline-on-migrate", () -> "false");

        registry.add("spring.mail.username", () -> "test@example.com");
        registry.add("spring.mail.password", () -> "test");
        registry.add("firebase.credentials", () -> "classpath:application.properties");
        registry.add("firebase.database-url", () -> "https://test.firebaseio.com");
    }

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected TestRestTemplate rest;
}
