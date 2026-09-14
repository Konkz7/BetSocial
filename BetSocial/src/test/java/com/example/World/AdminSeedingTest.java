package com.example.World;

import com.example.World.Users.UserRepository;
import com.example.World.Users.UserRole;
import com.example.World.Users.User_;
import com.example.World.Wallet.LedgerService;
import com.example.World.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        User_ original = users.findByUsername("admin").orElseThrow();
        long displaced = original.uid();
        jdbc.update("UPDATE User_ SET user_name = ?, email = ?, phone_number = ? WHERE uid = ?",
                "displaced-admin", "displaced-admin@example.com", "+" + PHONE, displaced);
        try {
            assertThat(users.findAll()).as("the table must not be empty, or the old "
                    + "seeding path would have created the admin anyway").isNotEmpty();
            assertThat(users.findByUsername("admin")).isEmpty();

            startup.onApplicationReady();

            User_ admin = users.findByUsername("admin").orElseThrow();
            assertThat(admin.uid()).isNotEqualTo(displaced);
            assertThat(admin.user_role()).isEqualTo(UserRole.ADMIN.toInt());
        } finally {
            // The container is shared by the whole suite, so put the account back
            // exactly as it was. Without this the test leaves a second admin
            // behind and the other tests here only pass in the right order.
            restoreAdmin(original, displaced);
        }
    }

    /** Drops any admin this test created and moves the original row back. */
    private void restoreAdmin(User_ original, long displaced) {
        users.findByUsername("admin").ifPresent(created -> {
            jdbc.update("DELETE FROM ledger_entry_ WHERE uid = ?", created.uid());
            jdbc.update("DELETE FROM User_ WHERE uid = ?", created.uid());
        });
        jdbc.update("UPDATE User_ SET user_name = ?, email = ?, phone_number = ? WHERE uid = ?",
                original.user_name(), original.email(), original.phone_number(), displaced);
    }

    @Test
    @DisplayName("a soft-deleted admin does not stop the application starting")
    void softDeletedAdminDoesNotBlockStartup() {
        // Every lookup in UserRepository filters deleted_at IS NULL, but
        // user_name, email and phone_number are UNIQUE with no deleted_at in the
        // constraint - so a soft-deleted admin is invisible to the seeding lookup
        // while still holding all three values. Seeding looks for the admin, does
        // not find it, tries to insert one, and collides.
        User_ original = users.findByUsername("admin").orElseThrow();
        long uid = original.uid();
        // deleted_at >= created_at is a CHECK constraint (V3), so this cannot be
        // an arbitrary marker value.
        jdbc.update("UPDATE User_ SET deleted_at = ? WHERE uid = ?",
                System.currentTimeMillis(), uid);
        try {
            assertThat(users.findByUsername("admin"))
                    .as("the row has to be invisible to the seeding lookup, or "
                            + "ensureAdmin returns early and never inserts")
                    .isEmpty();

            assertThatCode(startup::onApplicationReady)
                    .as("a deleted row holding the admin's username is worth a "
                            + "warning, not a refusal to start")
                    .doesNotThrowAnyException();
        } finally {
            // Drop anything the seed managed to create, then bring the original
            // row back - the container is shared with the rest of the suite, and
            // the other tests here expect a live admin.
            users.findByUsername("admin").ifPresent(created -> {
                if (created.uid() != uid) {
                    jdbc.update("DELETE FROM ledger_entry_ WHERE uid = ?", created.uid());
                    jdbc.update("DELETE FROM User_ WHERE uid = ?", created.uid());
                }
            });
            jdbc.update("UPDATE User_ SET deleted_at = NULL WHERE uid = ?", uid);
        }
    }

    @Test
    @DisplayName("a failure that is not a collision still stops the application")
    void nonCollisionFailurePropagates() {
        // Surviving the collision means catching DbActionExecutionException, which
        // is a plain RuntimeException - wide enough to swallow a database that is
        // simply not there. It must not: a deleted row is not the explanation for
        // that, and starting anyway would hide a dead database behind a warning
        // about a username.
        UserRepository failing = mock(UserRepository.class);
        when(failing.findByUsername("admin")).thenReturn(Optional.empty());
        when(failing.save(any(User_.class)))
                .thenThrow(new DataAccessResourceFailureException("the database is down"));

        Startup withDeadDatabase = new Startup(failing, mock(PasswordEncoder.class),
                mock(LedgerService.class), true, "");

        assertThatThrownBy(withDeadDatabase::onApplicationReady)
                .as("only an integrity violation is recoverable here")
                .isInstanceOf(DataAccessResourceFailureException.class);
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
