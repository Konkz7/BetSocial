package com.example.World.Comments;



import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface CommentLikeRepository extends ListCrudRepository<Commentlike_,Long> {


    @Query("SELECT * FROM CommentLike_ WHERE tid = :tid AND uid = :uid")
    List<Commentlike_> findByThreadAndUser(
            @Param("tid") Long tid,
            @Param("uid") Long uid);

    @Query("SELECT * FROM CommentLike_ WHERE cid = :cid AND uid = :uid")
    Optional<Commentlike_> findByCommentAndUser(
            @Param("cid") Long cid,
            @Param("uid") Long uid);









}
