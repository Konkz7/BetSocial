package com.example.World;

import com.example.World.Users.UserRepository;
import com.example.World.Users.UserRole;
import com.example.World.Wallet.LedgerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * What gets created on a server that is not somebody's laptop.
 *
 * The seeding was written for development and is right there: a fresh database
 * with nobody in it cannot be used. On a deployed server the same code is an
 * account called "admin" whose password is "password", reachable by anybody who
 * finds the address - which is not a seed, it is a way in.
 *
 * A plain unit test with mocks rather than an integration test: the whole
 * behaviour is which combination of two properties creates rows, and the shared
 * test container has one Startup that has already run.
 */
@DisplayName("Seeding outside development")
@ExtendWith(MockitoExtension.class)
class ProductionSeedingTest {

    @Mock UserRepository users;
    @Mock PasswordEncoder passwordEncoder;
    @Mock LedgerService ledgerService;

    private Startup startup(boolean devSeed, String adminPassword) {
        return new Startup(users, passwordEncoder, ledgerService, devSeed, adminPassword);
    }

    @Test
    @DisplayName("with seeding off and no password, nothing is created at all")
    void createsNothingInProduction() {
        when(users.findAll()).thenReturn(List.of());

        startup(false, "").onApplicationReady();

        verify(users, never()).save(any());
        verify(users, never()).findByUsername(anyString());
    }

    @Test
    @DisplayName("with seeding off, a supplied password still gets you an admin")
    void createsAnAdminFromASuppliedPassword() {
        when(users.findAll()).thenReturn(List.of());
        when(users.findByUsername("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("a-real-password")).thenReturn("hashed");
        when(users.save(any())).thenAnswer(call -> call.getArgument(0));

        startup(false, "a-real-password").onApplicationReady();

        // The admin, and nothing else - none of the ten demo accounts.
        verify(users).save(any());
        verify(passwordEncoder).encode("a-real-password");
        verify(passwordEncoder, never()).encode("password");
    }

    @Test
    @DisplayName("a supplied password is used for the admin, never the demo one")
    void neverFallsBackToTheDemoPassword() {
        when(users.findAll()).thenReturn(List.of());
        when(users.findByUsername("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(users.save(any())).thenAnswer(call -> call.getArgument(0));

        startup(true, "a-real-password").onApplicationReady();

        assertThat(UserRole.ADMIN.toInt())
                .as("sanity: the role constant the admin is created with")
                .isNotNull();
        verify(passwordEncoder, never()).encode("password");
    }

    @Test
    @DisplayName("development is unchanged - admin plus the demo accounts")
    void stillSeedsForDevelopment() {
        when(users.findAll()).thenReturn(List.of());
        when(users.findByUsername("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("hashed");
        when(users.save(any())).thenAnswer(call -> call.getArgument(0));

        startup(true, "").onApplicationReady();

        // One admin and ten demo accounts. If this ever changes, the README and
        // the warning in Startup are both wrong.
        verify(users, org.mockito.Mockito.times(11)).save(any());
    }

    @Test
    @DisplayName("a database that already has people is not re-seeded")
    void doesNotSeedDemoAccountsIntoAPopulatedDatabase() {
        when(users.findAll()).thenReturn(List.of(org.mockito.Mockito.mock(
                com.example.World.Users.User_.class)));
        when(users.findByUsername("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("hashed");
        when(users.save(any())).thenAnswer(call -> call.getArgument(0));

        startup(true, "").onApplicationReady();

        verify(users, org.mockito.Mockito.times(1)).save(any());
    }
}
