package com.example.World;

import com.example.World.Users.UserRepository;
import com.example.World.Users.UserRole;
import com.example.World.Users.User_;
import com.example.World.Wallet.LedgerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;


@Component
public class Startup {

    private static final Logger log = LoggerFactory.getLogger(Startup.class);

    /** Development sign-in for the admin UI. See README. */
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PHONE = "+2348039919669";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LedgerService ledgerService;

    public Startup(UserRepository userRepository, PasswordEncoder passwordEncoder,
                   LedgerService ledgerService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.ledgerService = ledgerService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {

        // Read this before seeding anything: ensureAdmin() below adds a row, so
        // asking afterwards would always say "not empty" and the demo users
        // would never be created on a fresh database.
        boolean freshDatabase = userRepository.findAll().isEmpty();

        // The admin is seeded on its own, every start, rather than as part of the
        // demo-user block. That block only runs against a completely empty table,
        // so a database that already had ordinary users in it - the normal state
        // after any testing - would never get an admin at all.
        ensureAdmin();

        if(freshDatabase) {
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

    /**
     * Guarantees there is always an admin account to sign in with. Looks the
     * account up by username rather than by role, so re-running against a
     * database that already has one is a no-op instead of a second admin.
     */
    private void ensureAdmin() {
        Optional<User_> existing = userRepository.findByUsername(ADMIN_USERNAME);
        if (existing.isPresent()) {
            Integer role = existing.get().user_role();
            if (role == null || role != UserRole.ADMIN.toInt()) {
                log.warn("A user named '{}' exists but is not an admin - the admin UI will be "
                        + "unreachable until its role is corrected.", ADMIN_USERNAME);
            }
            return;
        }
        try {
            createUser(ADMIN_USERNAME, ADMIN_PHONE, UserRole.ADMIN);
            log.info("Seeded the '{}' account.", ADMIN_USERNAME);
        } catch (DataIntegrityViolationException e) {
            // user_name, email and phone_number are unique regardless of
            // deleted_at, so a soft-deleted admin still holds those values.
            // Worth a warning, not worth refusing to start.
            log.warn("Could not seed the '{}' account - a deleted row still holds its username, "
                    + "email or phone number.", ADMIN_USERNAME);
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
                "",
                null,
                new Date().getTime(),          // Retain the creation timestamp
                null,
                role.toInt(), // Retain the user role
                "",
                "offline",
                null,              // default no wallet
                0.0,      // Default the balance to 0.0
                null            // Retain the version for optimistic locking
        );

        User_ saved = userRepository.save(userWithHashedPassword);

        // Seeded accounts get the same opening grant as a real registration, so a
        // fresh development database is usable rather than eleven people with
        // nothing to stake.
        ledgerService.grantOpeningBalance(saved.uid());
    }
}
