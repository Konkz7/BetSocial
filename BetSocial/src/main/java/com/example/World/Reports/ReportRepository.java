package com.example.World.Reports;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ReportRepository extends ListCrudRepository<Report_, Long> {

    /** The queue: whatever nobody has decided on yet, longest-waiting first. */
    @Query("SELECT * FROM Report_ WHERE resolved_at IS NULL ORDER BY created_at ASC")
    List<Report_> open();

    @Query("""
    SELECT EXISTS (
        SELECT 1 FROM Report_
        WHERE reporter_uid = :uid AND target_type = :type AND target_id = :id
    )
    """)
    boolean alreadyReported(@Param("uid") Long uid, @Param("type") String type,
                            @Param("id") Long id);

    /** How many people have reported the same thing - worth knowing before deciding. */
    @Query("SELECT COUNT(*) FROM Report_ WHERE target_type = :type AND target_id = :id")
    long countFor(@Param("type") String type, @Param("id") Long id);

    /**
     * Records the decision.
     *
     * Filters on resolved_at IS NULL so that two moderators pressing at the same
     * moment cannot both take the action - the second one updates nothing and the
     * caller sees it.
     */
    @Modifying
    @Transactional
    @Query("""
    UPDATE Report_
    SET resolved_at = :time, resolved_by = :moderator, action = :action
    WHERE rid = :rid AND resolved_at IS NULL
    """)
    int resolve(@Param("rid") Long rid, @Param("action") String action,
                @Param("moderator") Long moderator, @Param("time") Long time);

    /**
     * Closes every other open report about the same thing, once one has been
     * acted on. Ten people reporting one thread is one decision, not ten.
     */
    @Modifying
    @Transactional
    @Query("""
    UPDATE Report_
    SET resolved_at = :time, resolved_by = :moderator, action = :action
    WHERE target_type = :type AND target_id = :id AND resolved_at IS NULL
    """)
    int resolveAllFor(@Param("type") String type, @Param("id") Long id,
                      @Param("action") String action, @Param("moderator") Long moderator,
                      @Param("time") Long time);
}
