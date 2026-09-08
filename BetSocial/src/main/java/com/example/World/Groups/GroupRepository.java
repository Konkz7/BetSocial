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

    /**
     * Soft-deletes a conversation once the last member has gone.
     *
     * The messages are left in place rather than removed with it - they are the
     * record of what was said, and group_ is the only row that needs to know the
     * conversation is over.
     */
    @Modifying
    @Transactional
    @Query("UPDATE Group_ SET deleted_at = :time WHERE gid = :gid AND deleted_at IS NULL")
    int softDelete(@Param("gid") Long gid, @Param("time") Long time);

}
