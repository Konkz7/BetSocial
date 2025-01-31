package com.example.World.Messages;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface MessageRepository extends ListCrudRepository<Message_,Long> {

    @Query("SELECT * FROM Message_ WHERE gid = :gid ORDER BY created_at DESC")
    List<Message_> findMessagesByGidDesc(@Param("gid") Long gid);

}
