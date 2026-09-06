package com.example.World;

import com.example.World.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the Flyway migrations.
 *
 * Before Phase 2 the schema existed only on one developer's machine - schema.sql
 * was entirely commented out. These tests prove the migrations still build a
 * complete database from empty, which is what makes a clean clone runnable.
 */
@DisplayName("Flyway migrations")
class MigrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("apply cleanly to an empty database")
    void migrationsApply() {
        List<String> applied = jdbc.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY installed_rank",
                String.class);

        assertThat(applied).containsExactly("1", "2");
    }

    @Test
    @DisplayName("create every table the application queries")
    void allTablesExist() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class);

        assertThat(tables).contains(
                "user_", "thread_", "threadlike_", "comment_", "commentlike_",
                "bet_", "betsave_", "prediction_", "message_", "group_",
                "groupuser_", "follow_", "notification_", "card_",
                // Added by V2. BetRepository.makeDecision inserts into this table,
                // but it was missing from the database, so /superusers/approval and
                // /api/bets/decide both failed at runtime.
                "decision_log");
    }

    @Test
    @DisplayName("preserve the uniqueness guarantees registration relies on")
    void userUniquenessConstraints() {
        List<String> constrained = jdbc.queryForList("""
                SELECT a.attname
                FROM pg_constraint c
                JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY (c.conkey)
                WHERE c.conrelid = 'public.user_'::regclass AND c.contype = 'u'
                """, String.class);

        assertThat(constrained).contains("email", "phone_number", "user_name");
    }

    @Test
    @DisplayName("keep foreign keys intact")
    void foreignKeysExist() {
        Integer fks = jdbc.queryForObject(
                "SELECT count(*) FROM pg_constraint WHERE contype = 'f'", Integer.class);

        // 24 from the V1 baseline plus 2 added with decision_log in V2.
        assertThat(fks).isGreaterThanOrEqualTo(26);
    }
}
