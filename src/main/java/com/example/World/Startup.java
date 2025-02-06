package com.example.World;

import com.example.World.Users.UserRepository;
import com.example.World.Users.UserRole;
import com.example.World.Users.User_;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class Startup {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Startup(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {

        if(userRepository.findAll().isEmpty()) {
            createUser("admin", "+2348039919669", UserRole.ADMIN);

            createUser("john", "+2348012345678", UserRole.USER);
            createUser("jane", "+2348023456789", UserRole.USER);
            createUser("mike", "+2348034567890", UserRole.USER);
            createUser("emily", "+2348045678901", UserRole.USER);
            createUser("daniel", "+2348056789012", UserRole.USER);
            createUser("sophia", "+2348067890123", UserRole.USER);
            createUser("chris", "+2348078901234", UserRole.USER);
            createUser("olivia", "+2348089012345", UserRole.USER);
            createUser("william", "+2348090123456", UserRole.USER);
            createUser("amelia", "+2348101234567", UserRole.USER);
        }


    }

    private void createUser(String username, String phoneNumber, UserRole role) {

        String hashedPassword = passwordEncoder.encode("password");

        // Create a new User_ instance with the hashed password
        User_ userWithHashedPassword = new User_(
                null,                 // Retain the original UID (if null, it's auto-generated)
                username,           // Retain the username
                username + "03@live.co.uk",               // Retain the email
                hashedPassword,             // Use the hashed password
                phoneNumber,        // Retain the phone number
                null, // Token used for email verification
                true, // Boolean to check if the email is verified
                LocalDateTime.now(),          // Retain the creation timestamp
                null,
                role.toInt(),          // Retain the user role
                null            // Retain the version for optimistic locking
        );

        userRepository.save(userWithHashedPassword);
    }
}
