package com.example.World.External.Firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    /**
     * Service-account credentials, resolved as a Spring resource so the same code
     * works from the IDE and from a packaged jar. Defaults to the classpath copy;
     * override with e.g. FIREBASE_CREDENTIALS=file:/etc/betsocial/firebaseAPI.json.
     */
    private final Resource credentials;
    private final String databaseUrl;

    public FirebaseConfig(@Value("${firebase.credentials}") Resource credentials,
                          @Value("${firebase.database-url}") String databaseUrl) {
        this.credentials = credentials;
        this.databaseUrl = databaseUrl;
    }

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        try (InputStream serviceAccount = credentials.getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl(databaseUrl)
                    .build();

            return FirebaseApp.initializeApp(options);
        }
    }
}
