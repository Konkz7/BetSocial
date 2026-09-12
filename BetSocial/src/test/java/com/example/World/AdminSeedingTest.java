package com.example.World;

import com.example.World.Users.UserRepository;
import com.example.World.Users.UserRole;
import com.example.World.Users.User_;
import com.example.World.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * There is always an admin account to sign in with.
 *
 * The arrangement this replaces created the admin inside Startup's
 * `if (userRepository.findAll().isEmpty())` block, which only runs against a
 * completely empty user table. Register a single user before the first start -
 * or delete the admin once - and no admin would ever be created again, leaving
 * the admin UI unreachable with nothing in the codebase to recover it.
 */
@DisplayName("Admin seeding")
class AdminSeedingTest extends AbstractIntegrationTest {

    // See SecurityRegressionTest for the per-class phone_number blocks in use.
    private static final long PHONE = 2_354_000_000_000L;

    @Autowired Startup startup;
    @Autowired UserRepository users;

    @Test
    @DisplayName("the admin exists, and is actually an admin")
    void adminIsSeeded() {
        User_ admin = users.findByUsername("admin").orElseThrow();

        assertThat(admin.user_role())
                .as("a seeded account with the USER role would not reach the admin UI")
                .isEqualTo(UserRole.ADMIN.toInt());
    }

    @Test
    @DisplayName("an admin is created even when the user table is not empty")
    void adminIsSeededIntoAPopulatedDatabase() {
        // The regression, reproduced: users present, but nobody called "admin".
        // The row is moved aside rather than deleted - user_name, email and
        // phone_number are all UNIQUE, so the old values have to be freed before
        // a replacement can be inserted.
        long displaced = users.findByUsername("admin").orElseThrow().uid();
        jdbc.update("UPDATE User_ SET user_name = ?, email = ?, phone_number = ? WHERE uid = ?",
                "displaced-admin", "displaced-admin@example.com", "+" + PHONE, displaced);

        assertThat(users.findAll()).as("the table must not be empty, or the old "
                + "seeding path would have created the admin anyway").isNotEmpty();
        assertThat(users.findByUsername("admin")).isEmpty();

        startup.onApplicationReady();

        User_ admin = users.findByUsername("admin").orElseThrow();
        assertThat(admin.uid()).isNotEqualTo(displaced);
        assertThat(admin.user_role()).isEqualTo(UserRole.ADMIN.toInt());
    }

    @Test
    @DisplayName("starting again does not add a second admin")
    void seedingIsIdempotent() {
        long before = countAdminsNamedAdmin();

        startup.onApplicationReady();
        startup.onApplicationReady();

        assertThat(countAdminsNamedAdmin())
                .as("ensureAdmin runs on every start, so it has to be a no-op "
                        + "once the account is there")
                .isEqualTo(before);
    }

    private long countAdminsNamedAdmin() {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM User_ WHERE user_name = 'admin' AND deleted_at IS NULL",
                Long.class);
        return count == null ? 0 : count;
    }
}
