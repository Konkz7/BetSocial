package com.example.World.Comments;


import com.example.World.Messages.Message_;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface CommentRepository extends ListCrudRepository<Comment_,Long> {

    @Query("SELECT * FROM Comment_ WHERE tid = :tid AND deleted_at IS NULL ORDER BY created_at ASC")
    List<Comment_> findCommentsByTidAsc(@Param("tid") Long tid);

}
