package com.example.World.Groups;



import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.util.List;


public interface GroupRepository extends ListCrudRepository<Group_,Long> {

    @Modifying
    @Transactional
    @Query("UPDATE Group_ SET last_message = :message, last_time = :time WHERE gid = :gid")
    int updateGroupRecentData(@Param("gid") Long gid, @Param("message") String message , @Param("time") Long time);


}
