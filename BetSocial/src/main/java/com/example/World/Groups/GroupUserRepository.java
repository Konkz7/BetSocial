package com.example.World.Groups;



import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


public interface GroupUserRepository extends ListCrudRepository<Groupuser_,Long> {


    // Membership is soft-deleted, so every one of these is about *current*
    // membership and filters accordingly. A user who was removed and later added
    // back holds more than one row for the same group; only one is ever active.

    @Query("SELECT * FROM Groupuser_ WHERE uid= :uid AND deleted_at IS NULL")
    List<Groupuser_> findByUid(@Param("uid") Long uid);

    @Query("SELECT * FROM Groupuser_ WHERE gid= :gid AND uid= :uid AND deleted_at IS NULL")
    Optional<Groupuser_> findByGidandUid(@Param("gid") Long gid, @Param("uid") Long uid);

    /** Every current membership row for a conversation - who a message has to reach. */
    @Query("SELECT * FROM Groupuser_ WHERE gid= :gid AND deleted_at IS NULL")
    List<Groupuser_> findByGid(@Param("gid") Long gid);

    /**
     * The same, for several conversations at once.
     *
     * The conversation list needs the members of every conversation a user is in;
     * asking per conversation would make that cost grow with how much they use the
     * app.
     */
    @Query("SELECT * FROM Groupuser_ WHERE gid IN (:gids) AND deleted_at IS NULL")
    List<Groupuser_> findByGidIn(@Param("gids") List<Long> gids);

    /**
     * Memberships that have ended - the conversations a user used to be in.
     *
     * Only the most recent ending per group, so somebody removed and re-removed
     * appears once rather than once per departure.
     */
    @Query("""
    SELECT DISTINCT ON (gid) *
    FROM Groupuser_
    WHERE uid = :uid AND deleted_at IS NOT NULL
    ORDER BY gid, deleted_at DESC
    """)
    List<Groupuser_> findPastMembershipsByUid(@Param("uid") Long uid);

    @Modifying
    @Transactional
    @Query("UPDATE Groupuser_ SET deleted_at = :time WHERE guid = :guid AND deleted_at IS NULL")
    int softDelete(@Param("guid") Long guid, @Param("time") Long time);

    @Modifying
    @Transactional
    @Query("UPDATE Groupuser_ SET last_read_timestamp = :now WHERE guid = :guid")
    int updateReadTimestamp(@Param("guid") Long guid , @Param("now") Long now);

    /**
     * Moves a read timestamp forward, never back.
     *
     * Used when a message arrives at somebody who already has the conversation
     * open. The guard matters because that happens concurrently with the reader's
     * own updates, and a message that took slightly longer to save must not undo a
     * later one.
     */
    @Modifying
    @Transactional
    @Query("UPDATE Groupuser_ SET last_read_timestamp = :now WHERE guid = :guid AND last_read_timestamp < :now")
    int advanceReadTimestamp(@Param("guid") Long guid, @Param("now") Long now);

    @Modifying
    @Transactional
    @Query("UPDATE Groupuser_ SET administrator = :administrator WHERE guid = :guid")
    int updateAdministrator(@Param("guid") Long guid, @Param("administrator") boolean administrator);

}
