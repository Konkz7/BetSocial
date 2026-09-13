package com.example.World.External.Firebase;

import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    /**
     * Service-account credentials, resolved as a Spring resource so the same code
     * works from the IDE and from a packaged jar. Defaults to the classpath copy;
     * override with e.g. FIREBASE_CREDENTIALS=file:/etc/betsocial/firebaseAPI.json.
     */
    private final Resource credentials;

    /**
     * The service-account JSON itself, for somewhere there is no file to point at.
     *
     * A deployed container has no copy of firebaseAPI.json - it is gitignored,
     * and .dockerignore keeps it out of the build context on purpose, because a
     * private key baked into an image is a private key shipped to wherever that
     * image goes. Hosts hand secrets over as environment variables, so the key
     * arrives as one and is read from here.
     *
     * Takes precedence over the file when set, so the file stays the local path
     * and nothing changes for development.
     */
    private final String credentialsJson;

    private final String databaseUrl;

    public FirebaseConfig(@Value("${firebase.credentials}") Resource credentials,
                          @Value("${firebase.credentials-json:}") String credentialsJson,
                          @Value("${firebase.database-url}") String databaseUrl) {
        this.credentials = credentials;
        this.credentialsJson = credentialsJson == null ? "" : credentialsJson.trim();
        this.databaseUrl = databaseUrl;
    }

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        try (InputStream serviceAccount = openCredentials()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl(databaseUrl)
                    .build();

            return FirebaseApp.initializeApp(options);
        }
    }

    /**
     * The credentials, from the environment if they are there and the file if not.
     *
     * Nothing about either is logged. The only thing worth saying out loud is
     * which of the two was used, and even that names no value.
     */
    private InputStream openCredentials() throws IOException {
        if (!credentialsJson.isBlank()) {
            log.info("Firebase credentials loaded from the environment");
            return new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8));
        }

        log.info("Firebase credentials loaded from {}", credentials.getDescription());
        return credentials.getInputStream();
    }
}
