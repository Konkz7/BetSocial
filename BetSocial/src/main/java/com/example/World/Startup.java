package com.example.World;

import com.example.World.Users.UserRepository;
import com.example.World.Users.UserRole;
import com.example.World.Users.User_;
import com.example.World.Wallet.LedgerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;


@Component
public class Startup {

    private static final Logger log = LoggerFactory.getLogger(Startup.class);

    /** Development sign-in for the admin UI. See README. */
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PHONE = "+2348039919669";

    /** What the demo accounts sign in with. Fine locally, fatal anywhere else. */
    private static final String DEMO_PASSWORD = "password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LedgerService ledgerService;

    /**
     * Whether to create the demo accounts.
     *
     * On by default, because a fresh local database with nobody in it is not
     * usable and that is the common case for this flag. A deployment turns it
     * off - see application-prod.properties, which does.
     */
    private final boolean devSeed;

    /**
     * The admin's password, when one has been supplied.
     *
     * Empty means "use the demo password", which is only allowed to happen while
     * demo seeding is on. With seeding off and no password given, no admin is
     * created at all - an account called "admin" whose password is "password" on
     * a public server is worse than having no admin.
     */
    private final String adminPassword;

    public Startup(UserRepository userRepository, PasswordEncoder passwordEncoder,
                   LedgerService ledgerService,
                   @Value("${betsocial.dev-seed:true}") boolean devSeed,
                   @Value("${betsocial.admin-password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.ledgerService = ledgerService;
        this.devSeed = devSeed;
        this.adminPassword = adminPassword == null ? "" : adminPassword.trim();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {

        // Read this before seeding anything: ensureAdmin() below adds a row, so
        // asking afterwards would always say "not empty" and the demo users
        // would never be created on a fresh database.
        boolean freshDatabase = userRepository.findAll().isEmpty();

        // Said before anything is created, so it is at the top of the log where
        // somebody bringing a server up will see it rather than buried under the
        // seeding it is warning about.
        warnAboutDefaultCredentials();

        // The admin is seeded on its own, every start, rather than as part of the
        // demo-user block. That block only runs against a completely empty table,
        // so a database that already had ordinary users in it - the normal state
        // after any testing - would never get an admin at all.
        ensureAdmin();

        if(freshDatabase && devSeed) {
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
     * Says, once and loudly, that this server has accounts anybody can sign into.
     *
     * The seeding was written for a laptop and is correct there. The failure it
     * guards against is nobody remembering it exists on the day the server stops
     * being a laptop.
     */
    private void warnAboutDefaultCredentials() {
        if (devSeed && adminPassword.isEmpty()) {
            log.warn("Development seeding is ON: '{}' and the demo accounts exist with the "
                    + "password '{}'. Set DEV_SEED=false (or SPRING_PROFILES_ACTIVE=prod) "
                    + "before this is reachable from anywhere but this machine.",
                    ADMIN_USERNAME, DEMO_PASSWORD);
        }
    }

    /**
     * The password new seeded accounts get, or empty when none may be created.
     *
     * With seeding off and no ADMIN_PASSWORD given there is deliberately no
     * fallback: the alternative is a known password on a public server, and a
     * missing admin is the safer of the two failures - it can be fixed with one
     * SQL statement, which the log says.
     */
    private Optional<String> seedPassword() {
        if (!adminPassword.isEmpty()) {
            return Optional.of(adminPassword);
        }
        return devSeed ? Optional.of(DEMO_PASSWORD) : Optional.empty();
    }

    /**
     * Guarantees there is an admin account to sign in with, when one may be
     * created at all. Looks the account up by username rather than by role, so
     * re-running against a database that already has one is a no-op instead of a
     * second admin.
     */
    private void ensureAdmin() {
        if (seedPassword().isEmpty()) {
            log.info("No admin seeded: development seeding is off and no ADMIN_PASSWORD was set. "
                    + "Set ADMIN_PASSWORD and restart, or promote an existing account with "
                    + "UPDATE user_ SET user_role = {} WHERE user_name = '<name>'.",
                    UserRole.ADMIN.toInt());
            return;
        }

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
        } catch (DbActionExecutionException | DataAccessException e) {
            // user_name, email and phone_number are unique regardless of
            // deleted_at, so a soft-deleted admin still holds those values.
            // Worth a warning, not worth refusing to start.
            //
            // Both types have to be caught, because save() does not let the
            // translated DataIntegrityViolationException out: Spring Data JDBC
            // catches everything the insert throws and rethrows it wrapped in a
            // DbActionExecutionException, which extends RuntimeException and is
            // not a DataAccessException at all. Catching only the translated
            // exception - as this did - caught nothing, and the collision this
            // means to survive took the application down on startup instead.
            if (!hasCause(e, DataIntegrityViolationException.class)) {
                // Not the collision: a database that cannot be reached, or any
                // other failure that a missing admin is not the explanation for.
                // Starting anyway would hide it behind a warning about a deleted
                // row that may not exist.
                throw e;
            }
            // The constraint name only appears in the driver's message at the
            // bottom of the chain, so the warning quotes it rather than guessing
            // which of the three values collided.
            log.warn("Could not seed the '{}' account - a deleted row still holds its username, "
                    + "email or phone number: {}", ADMIN_USERNAME,
                    NestedExceptionUtils.getMostSpecificCause(e).getMessage());
        }
    }

    /** Whether {@code type} appears anywhere in the exception's cause chain. */
    private static boolean hasCause(Throwable thrown, Class<? extends Throwable> type) {
        for (Throwable cause = thrown; cause != null; cause = cause.getCause()) {
            if (type.isInstance(cause)) {
                return true;
            }
            // A throwable is not supposed to be its own cause; this is a cheap
            // guarantee that a malformed one cannot spin here forever. Startup
            // is the worst place to hang, because nothing is serving yet.
            if (cause.getCause() == cause) {
                break;
            }
        }
        return false;
    }

    private void createUser(String username, String phoneNumber, UserRole role) {

        // seedPassword() is never empty here: ensureAdmin returns early when it is,
        // and the demo block only runs while devSeed is on.
        String hashedPassword = passwordEncoder.encode(seedPassword().orElse(DEMO_PASSWORD));

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
