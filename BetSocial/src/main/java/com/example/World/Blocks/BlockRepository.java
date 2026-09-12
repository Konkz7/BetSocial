package com.example.World.Blocks;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface BlockRepository extends ListCrudRepository<Block_, Long> {

    /**
     * Everybody invisible to this user, in one query.
     *
     * A block is mutual: whoever pressed the button, neither sees the other. So
     * the table is read from both sides and the *other* party's id is returned
     * each time. Callers get a single set to filter against and never have to
     * remember which direction a given row was stored in - which is exactly the
     * kind of detail that gets forgotten at one call site out of five and
     * becomes a way to keep seeing somebody who blocked you.
     */
    @Query("""
    SELECT CASE WHEN blocker_uid = :uid THEN blocked_uid ELSE blocker_uid END
    FROM Block_
    WHERE blocker_uid = :uid OR blocked_uid = :uid
    """)
    List<Long> invisibleTo(@Param("uid") Long uid);

    /** The people this user blocked, for the screen that lists them. */
    @Query("SELECT * FROM Block_ WHERE blocker_uid = :uid ORDER BY created_at DESC")
    List<Block_> blockedBy(@Param("uid") Long uid);

    @Query("SELECT EXISTS (SELECT 1 FROM Block_ WHERE blocker_uid = :blocker AND blocked_uid = :blocked)")
    boolean exists(@Param("blocker") Long blocker, @Param("blocked") Long blocked);

    /**
     * Only the person who blocked can unblock, which is why this filters on
     * blocker_uid rather than taking a blid.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Block_ WHERE blocker_uid = :blocker AND blocked_uid = :blocked")
    int unblock(@Param("blocker") Long blocker, @Param("blocked") Long blocked);
}
