package com.example.World;

import com.example.World.Users.UserRepository;
import com.example.World.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sample data stays off unless somebody asks for it.
 *
 * SampleData writes a few hundred rows. That is exactly what is wanted when
 * testing pagination by hand and exactly what is not wanted anywhere else, so
 * the default matters more than the feature does - a flipped default would
 * quietly put sixty accounts and seventy-five threads into a real database on
 * the next restart.
 */
@DisplayName("Sample data")
class SampleDataTest extends AbstractIntegrationTest {

    @Autowired UserRepository users;

    @Test
    @DisplayName("does not run unless switched on")
    void isOffByDefault() {
        // The test context sets no sample-data property, so this is the same
        // path a deployment takes. If the default ever changes, the seeded
        // account names appear here first.
        assertThat(users.findByUsername("sample0"))
                .as("sample accounts must only exist when SAMPLE_DATA is set")
                .isEmpty();

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM User_ WHERE user_name LIKE 'sample%'", Long.class))
                .isZero();
    }
}
