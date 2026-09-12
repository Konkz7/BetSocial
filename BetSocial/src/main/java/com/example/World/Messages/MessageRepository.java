package com.example.World.Messages;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


public interface MessageRepository extends ListCrudRepository<Message_,Long> {

    @Query("SELECT * FROM Message_ WHERE gid = :gid ORDER BY created_at ASC")
    List<Message_> findMessagesByGidAsc(@Param("gid") Long gid);

    /**
     * One page of a conversation, newest first.
     *
     * Newest first because that is the end people open a chat at. Paging forwards
     * from the oldest message would mean fetching an entire history before
     * showing the one message somebody just received.
     *
     * Keyset on (created_at, mid), like the feed. The mid is part of the cursor
     * because created_at is not unique and a conversation is where that stops
     * being a theoretical worry: two people replying at once, or a client sending
     * a burst, land in the same millisecond regularly. A created_at-only cursor
     * would step over all but one of them.
     *
     * A null cursor means the most recent page.
     */
    @Query("""
    SELECT * FROM Message_
    WHERE gid = :gid
      AND (CAST(:cursorCreatedAt AS bigint) IS NULL
           OR (created_at, mid) < (CAST(:cursorCreatedAt AS bigint), CAST(:cursorMid AS bigint)))
    ORDER BY created_at DESC, mid DESC
    LIMIT :limit
    """)
    List<Message_> findMessagePage(@Param("gid") Long gid,
                                   @Param("cursorCreatedAt") Long cursorCreatedAt,
                                   @Param("cursorMid") Long cursorMid,
                                   @Param("limit") int limit);

    // No read-state queries here any more. Reading is something a person does to a
    // conversation, recorded once on their membership row as last_read_timestamp -
    // not a column flipped on every message they have seen.


    @Modifying
    @Transactional
    @Query("UPDATE Message_ SET description = 'This message was deleted' , deleted_at = :time , media_type = 0 WHERE mid = :mid")
    int softDelete(@Param("mid") Long mid,@Param("time") Long time);

    /**
     * Everything one person has sent, for their data export. Soft-deleted rows
     * included - see CommentRepository.findAllByUser for why.
     */
    @Query("SELECT * FROM Message_ WHERE uid = :uid ORDER BY created_at ASC")
    List<Message_> findAllByUser(@Param("uid") Long uid);




}
