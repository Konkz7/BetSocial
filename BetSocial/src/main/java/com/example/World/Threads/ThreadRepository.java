package com.example.World.Threads;


import com.example.World.Users.User_;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Optional;

public interface ThreadRepository extends ListCrudRepository<Thread_, Long> {


    @Modifying
    @Transactional
    @Query("UPDATE Thread_ SET  deleted_at = :deleted_at WHERE tid = :id AND deleted_at IS NULL")
    int remove(
            @Param("id") Long id,
            @Param("deleted_at") Long deleted_at);


    @Modifying
    @Transactional
    @Query("UPDATE Thread_ SET  likes = likes + 1 WHERE tid = :id AND deleted_at IS NULL")
    int increment(
            @Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Thread_ SET  likes = likes - 1 WHERE tid = :id AND deleted_at IS NULL")
    int decrement(
            @Param("id") Long id);

    @Query("SELECT * FROM Thread_ WHERE uid = :uid AND deleted_at IS NULL ORDER BY created_at DESC")
    List<Thread_> findAllUserThreads(@Param("uid") Long uid);

    /**
     * One person's threads, as a particular viewer is allowed to see them.
     *
     * Carries the same visibility rules as findFeedPage. Not paginated: this is
     * bounded by how much one person has posted, and a profile is scrolled to the
     * end far more often than a feed is. The rules are repeated rather than shared
     * because a Spring Data @Query is a string and there is nowhere to put a
     * fragment - FeedVisibilityTest runs against both paths, so the two cannot
     * drift without a test failing.
     */
    @Query("""
    SELECT t.* FROM Thread_ t
    JOIN User_ u ON u.uid = t.uid
    WHERE t.deleted_at IS NULL
      AND t.uid = :target
      AND NOT EXISTS (
          SELECT 1 FROM Block_ b
          WHERE (b.blocker_uid = :viewer AND b.blocked_uid = t.uid)
             OR (b.blocker_uid = t.uid AND b.blocked_uid = :viewer))
      AND (t.uid = :viewer
           OR t.is_private = false
           OR (EXISTS (SELECT 1 FROM Follow_ f
                       WHERE f.request_id = :viewer AND f.receive_id = t.uid)
           AND EXISTS (SELECT 1 FROM Follow_ f
                       WHERE f.request_id = t.uid AND f.receive_id = :viewer)))
    ORDER BY t.created_at DESC, t.tid DESC
    """)
    List<Thread_> findUserThreadsVisibleTo(@Param("viewer") Long viewer,
                                           @Param("target") Long target);

    /**
     * One page of the feed, with every visibility rule applied in SQL.
     *
     * The rules used to run in Java after the query, which is fine when the query
     * returns everything and wrong the moment it returns a page: LIMIT 20 would
     * fetch twenty rows and hand back however many survived filtering, so pages
     * came out uneven and a page could arrive empty while more threads existed -
     * which a client reads as the end of the feed. Threads would simply go
     * missing. Deciding visibility here means a page is the size it says it is
     * and the cursor is always right.
     *
     * The rules, in order:
     *   - neither party has blocked the other, in either direction
     *   - a private thread needs a mutual follow, unless it is the viewer's own
     *
     * Keyset pagination on (created_at, tid), not OFFSET. An offset into a feed
     * ordered by created_at DESC skips or repeats rows whenever something is
     * posted between two requests, which on an active feed is most of the time.
     * The tid is part of the cursor because created_at is not unique - two
     * threads in the same millisecond would make a single-column cursor step over
     * one of them.
     *
     * A null cursor means the first page.
     *
     * The join no longer requires the author's account to be live, because
     * deleted_at now means two different things. A suspended account has every
     * one of its threads soft-deleted by the moderator action itself, so it is
     * already excluded by t.deleted_at - whereas somebody who deleted their own
     * account keeps their content, attributed to a tombstone, which is the whole
     * point of scrubbing rather than erasing. Filtering on the author here would
     * have made the second case behave like the first.
     */
    @Query("""
    SELECT t.* FROM Thread_ t
    JOIN User_ u ON u.uid = t.uid
    WHERE t.deleted_at IS NULL
      AND NOT EXISTS (
          SELECT 1 FROM Block_ b
          WHERE (b.blocker_uid = :viewer AND b.blocked_uid = t.uid)
             OR (b.blocker_uid = t.uid AND b.blocked_uid = :viewer))
      AND (t.uid = :viewer
           OR t.is_private = false
           OR (EXISTS (SELECT 1 FROM Follow_ f
                       WHERE f.request_id = :viewer AND f.receive_id = t.uid)
           AND EXISTS (SELECT 1 FROM Follow_ f
                       WHERE f.request_id = t.uid AND f.receive_id = :viewer)))
      AND (CAST(:cursorCreatedAt AS bigint) IS NULL
           OR (t.created_at, t.tid) < (CAST(:cursorCreatedAt AS bigint), CAST(:cursorTid AS bigint)))
    ORDER BY t.created_at DESC, t.tid DESC
    LIMIT :limit
    """)
    List<Thread_> findFeedPage(@Param("viewer") Long viewer,
                               @Param("cursorCreatedAt") Long cursorCreatedAt,
                               @Param("cursorTid") Long cursorTid,
                               @Param("limit") int limit);
/*
    List<Thread> findAll();

    Optional<Thread> findByTid(Integer tid);

    void create(Thread thread);

    void update( Thread thread, Integer tid);

    void delete(Integer tid);

    int count();

    void saveAll(List<Thread> threads);

 */


}
