package com.example.World.Firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {
    @Bean
    public void initializeFirebase() throws IOException {
        FileInputStream serviceAccount = new FileInputStream("src/main/resources/firebaseAPI.json");

        FirebaseOptions.Builder builder = FirebaseOptions.builder();
        FirebaseOptions options = builder
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }
    }
}
