package com.example.World.Comments;


import com.example.World.Messages.Message_;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


public interface CommentRepository extends ListCrudRepository<Comment_,Long> {

    @Query("SELECT * FROM Comment_ WHERE tid = :tid ORDER BY created_at DESC")
    List<Comment_> findCommentsByTidAsc(@Param("tid") Long tid);


    @Query("SELECT * FROM Comment_ WHERE tid = :tid ")
    List<Comment_> findByThread(
            @Param("tid") Long tid);


    @Modifying
    @Transactional
    @Query("UPDATE Comment_ SET  likes = likes + 1 WHERE cid = :id AND deleted_at IS NULL")
    int increment(
            @Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Comment_ SET  likes = likes - 1 WHERE cid = :id AND deleted_at IS NULL")
    int decrement(
            @Param("id") Long id);

    @Query("SELECT * FROM Comment_ WHERE parent_cid = :id AND deleted_at IS NULL")
    List<Comment_> findCommentsByParentCID(@Param("id") Long parent_cid);



    @Modifying
    @Transactional
    @Query("UPDATE Comment_ SET  deleted_at = :time , description = '[deleted]' WHERE cid = :id AND deleted_at IS NULL")
    int softDelete(
            @Param("id") Long id,
            @Param("time") Long time);

}
