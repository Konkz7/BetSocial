package com.example.World;

import com.example.World.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

        assertThat(applied).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13");
    }

    @Test
    @DisplayName("create every table the application queries")
    void allTablesExist() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class);

        assertThat(tables).contains(
                "user_", "thread_", "threadlike_", "comment_", "commentlike_", "block_", "report_",
                "bet_", "betsave_", "prediction_", "message_", "group_",
                "groupuser_", "follow_", "notification_", "card_",
                // Added by V2. BetRepository.makeDecision inserts into this table,
                // but it was missing from the database, so /superusers/approval and
                // /api/bets/decide both failed at runtime.
                "decision_log");
    }

    @Test
    @DisplayName("let a conversation go unnamed, but not be named blank")
    void conversationNameIsOptional() {
        // A direct conversation has no name - it is titled from whoever else is in
        // it. V1 forbade that outright, which is why createDMGroup invented a name
        // from the two uids concatenated just to get a row in.
        Long gid = jdbc.queryForObject(
                "INSERT INTO group_ (group_name, created_at) VALUES (NULL, ?) RETURNING gid",
                Long.class, System.currentTimeMillis());

        assertThat(gid).isNotNull();
        assertThat(jdbc.queryForObject(
                "SELECT group_name FROM group_ WHERE gid = ?", String.class, gid))
                .as("NULL must stay NULL rather than picking up a default")
                .isNull();

        // Optional is not the same as blank: a named group still needs a real name.
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO group_ (group_name, created_at) VALUES ('', ?)",
                System.currentTimeMillis()))
                .hasMessageContaining("chk_group_name_not_blank");
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
    @DisplayName("enforce that a row cannot be deleted before it was created")
    void softDeleteTimestampChecks() {
        List<String> checks = jdbc.queryForList(
                "SELECT conname FROM pg_constraint WHERE contype = 'c' AND conname LIKE 'chk_%_deleted_after_created'",
                String.class);

        // Named rather than counted: a bare size told you the number had moved but
        // not which table had gained or lost its guarantee, and it needed editing
        // every time a table became soft-deletable.
        assertThat(checks).containsExactlyInAnyOrder(
                "chk_thread_deleted_after_created",
                "chk_comment_deleted_after_created",
                "chk_bet_deleted_after_created",
                "chk_message_deleted_after_created",
                "chk_prediction_deleted_after_created",
                "chk_user_deleted_after_created",
                "chk_groupuser_deleted_after_created");
    }

    @Test
    @DisplayName("reject a row deleted before it was created")
    void softDeleteCheckActuallyRejects() {
        // Existence is not enough - prove the constraint bites.
        Long uid = jdbc.queryForObject("SELECT uid FROM user_ LIMIT 1", Long.class);
        Long now = System.currentTimeMillis();

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO thread_ (uid, title, media_type, category, likes,
                                     created_at, deleted_at, is_private, t_version)
                VALUES (?, 'backwards timestamps', 0, 'test', 0, ?, ?, false, 0)
                """, uid, now, now - 1_000))
                .as("deleted_at before created_at must be refused by the database")
                .hasMessageContaining("chk_thread_deleted_after_created");
    }

    @Test
    @DisplayName("drop the columns the group work superseded")
    void supersededColumnsAreGone() {
        // Each of these encoded something the membership rows already held, or held
        // one person's answer to a question about several. They were left in place
        // change by change so the replacements could be used for real first.
        assertThat(columnsOf("group_"))
                .doesNotContain("sort", "deleted_at")
                .as("a conversation is still named and still tracks its last message")
                .contains("group_name", "last_mid");

        assertThat(columnsOf("groupuser_"))
                .doesNotContain("other_uid")
                .as("read state and membership's own soft delete stay")
                .contains("last_read_timestamp", "deleted_at", "administrator");

        assertThat(columnsOf("message_"))
                .doesNotContain("recipient_id", "is_read")
                .as("a message still belongs to a conversation and can still be soft-deleted")
                .contains("gid", "uid", "deleted_at");

        // A different table's is_read, still written and still read.
        assertThat(columnsOf("notification_")).contains("is_read");
    }

    private List<String> columnsOf(String table) {
        return jdbc.queryForList(
                "SELECT column_name FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = ?",
                String.class, table);
    }

    @Test
    @DisplayName("keep foreign keys intact")
    void foreignKeysExist() {
        Integer fks = jdbc.queryForObject(
                "SELECT count(*) FROM pg_constraint WHERE contype = 'f'", Integer.class);

        // 24 from the V1 baseline plus 2 added with decision_log in V2, less
        // fk_recipient_id, which V8 dropped along with message_.recipient_id.
        assertThat(fks).isGreaterThanOrEqualTo(25);
    }
}
