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

    @Query("SELECT * FROM Message_ WHERE gid = :gid AND is_read = false ORDER BY created_at ASC")
    List<Message_> findMessagesByGidAscAndRead(@Param("gid") Long gid);

    @Modifying
    @Transactional
    @Query("UPDATE Message_ SET is_read = true WHERE mid = :mid")
    int updateReadReceipt(@Param("mid") Long mid);

}
