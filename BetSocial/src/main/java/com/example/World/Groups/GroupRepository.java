package com.example.World.Groups;



import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


public interface GroupRepository extends ListCrudRepository<Group_,Long> {

    @Modifying
    @Transactional
    @Query("UPDATE Group_ SET last_mid = :mid WHERE gid = :gid")
    int updateGroupRecentData(@Param("gid") Long gid, @Param("mid") Long mid);

    /**
     * The direct conversation between two people, if they already have one.
     *
     * Used so that opening a chat from a profile reuses the existing conversation
     * instead of starting a second one beside it. A direct conversation is an
     * unnamed one whose active membership is exactly this pair - checked by
     * counting, so a group that happens to contain both of them cannot match.
     */
    @Query("""
    SELECT g.* FROM Group_ g
    WHERE g.group_name IS NULL
      AND (
        SELECT count(*) FROM Groupuser_ gu
        WHERE gu.gid = g.gid AND gu.deleted_at IS NULL
      ) = 2
      AND EXISTS (
        SELECT 1 FROM Groupuser_ gu
        WHERE gu.gid = g.gid AND gu.uid = :uid AND gu.deleted_at IS NULL
      )
      AND EXISTS (
        SELECT 1 FROM Groupuser_ gu
        WHERE gu.gid = g.gid AND gu.uid = :otherUid AND gu.deleted_at IS NULL
      )
    ORDER BY g.gid
    LIMIT 1
    """)
    Optional<Group_> findDirectConversation(@Param("uid") Long uid,
                                            @Param("otherUid") Long otherUid);

    @Modifying
    @Transactional
    // No deleted_at guard: a conversation is only ever hard-deleted, so a row that
    // is still here is live by definition.
    @Query("UPDATE Group_ SET group_name = :name WHERE gid = :gid")
    int updateName(@Param("gid") Long gid, @Param("name") String name);

    // No soft delete here on purpose. A group is only ever hard-deleted, via
    // ListCrudRepository.deleteById, and V1's foreign keys cascade that to
    // groupuser_ and message_. Soft-deleting it would leave the membership rows
    // in place pointing at a conversation nobody can reach, and hard-deleting it
    // automatically would destroy the soft-deleted memberships that are the only
    // record of who was ever in it - so nothing deletes a group on its own.

}
