package com.example.World;

import com.example.World.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V6 clears the names of conversations created by the old createDMGroup, which
 * invented one from the two participants' uids concatenated - users 8 and 5 gave
 * "85". A name is now what marks a conversation as a group, so those left every
 * existing private chat listed as a number and treated as a group.
 *
 * Flyway has already run against an empty database by the time these execute, so
 * there is nothing for it to have backfilled. What is worth testing is the
 * predicate rather than the run: it is an UPDATE that clears data, and getting it
 * too broad would wipe the names of real groups. So the statement is applied here
 * to rows built to look like the cases it has to tell apart.
 */
@DisplayName("Legacy conversation names")
class LegacyConversationNameTest extends AbstractIntegrationTest {

    /** Verbatim from V6__clear_legacy_direct_conversation_names.sql. */
    private static final String BACKFILL = """
            UPDATE public.group_ g
            SET group_name = NULL
            WHERE g.group_name IS NOT NULL
              AND EXISTS (
                SELECT 1
                FROM public.groupuser_ a
                JOIN public.groupuser_ b ON b.gid = a.gid AND b.uid <> a.uid
                WHERE a.gid = g.gid
                  AND g.group_name = a.uid::text || b.uid::text
              )
            """;

    @Test
    @DisplayName("clear an invented direct-message name but keep a real group name")
    void clearsOnlyTheInventedNames() {
        Long alice = user("legacy-alice", "+2360000000001");
        Long bob = user("legacy-bob", "+2360000000002");
        Long carol = user("legacy-carol", "+2360000000003");

        // What createDMGroup produced: the two uids run together.
        Long invented = conversation(alice + "" + bob, alice, bob);

        // A real group of the same two people. Its name is not their uids, so it
        // has to survive - this is the case a broader predicate would destroy.
        Long realGroup = conversation("Fantasy League", alice, bob);

        // A group whose name is digits but not these members' uids. Nothing to do
        // with them, and equally must survive.
        Long numericName = conversation("2024", alice, carol);

        jdbc.update(BACKFILL);

        assertThat(nameOf(invented))
                .as("the invented name is what left a chat showing as a number")
                .isNull();
        assertThat(nameOf(realGroup))
                .as("a real group of the same two people keeps its name")
                .isEqualTo("Fantasy League");
        assertThat(nameOf(numericName))
                .as("a digits-only group name is not evidence of anything")
                .isEqualTo("2024");
    }

    @Test
    @DisplayName("leave a conversation that is already unnamed alone")
    void unnamedConversationsAreUntouched() {
        Long dave = user("legacy-dave", "+2360000000004");
        Long erin = user("legacy-erin", "+2360000000005");

        Long alreadyDirect = conversation(null, dave, erin);

        assertThat(jdbc.update(BACKFILL))
                .as("a conversation created since V5 is already correct")
                .isZero();
        assertThat(nameOf(alreadyDirect)).isNull();
    }

    private String nameOf(Long gid) {
        return jdbc.queryForObject("SELECT group_name FROM group_ WHERE gid = ?", String.class, gid);
    }

    private Long conversation(String name, Long... members) {
        Long now = System.currentTimeMillis();
        Long gid = jdbc.queryForObject(
                "INSERT INTO group_ (group_name, created_at) VALUES (?, ?) RETURNING gid",
                Long.class, name, now);

        for (Long uid : members) {
            jdbc.update("""
                    INSERT INTO groupuser_ (gid, uid, created_at, last_read_timestamp, administrator)
                    VALUES (?, ?, ?, ?, false)
                    """, gid, uid, now, now);
        }

        return gid;
    }

    private Long user(String name, String phone) {
        return jdbc.queryForObject("""
                INSERT INTO user_ (user_name, email, pass_word, phone_number, is_verified,
                                   bio, created_at, user_role, status, balance)
                VALUES (?, ?, 'x', ?, true, '', ?, 0, 'offline', 0)
                RETURNING uid
                """, Long.class, name, name + "@example.test", phone, System.currentTimeMillis());
    }
}
