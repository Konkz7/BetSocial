package com.example.World.Groups;



import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


public interface GroupRepository extends ListCrudRepository<Group_,Long> {

    @Modifying
    @Transactional
    @Query("UPDATE Group_ SET last_mid = :mid WHERE gid = :gid")
    int updateGroupRecentData(@Param("gid") Long gid, @Param("mid") Long mid);

    @Modifying
    @Transactional
    @Query("UPDATE Group_ SET group_name = :name WHERE gid = :gid AND deleted_at IS NULL")
    int updateName(@Param("gid") Long gid, @Param("name") String name);

    // No soft delete here on purpose. A group is only ever hard-deleted, via
    // ListCrudRepository.deleteById, and V1's foreign keys cascade that to
    // groupuser_ and message_. Soft-deleting it would leave the membership rows
    // in place pointing at a conversation nobody can reach, and hard-deleting it
    // automatically would destroy the soft-deleted memberships that are the only
    // record of who was ever in it - so nothing deletes a group on its own.

}
