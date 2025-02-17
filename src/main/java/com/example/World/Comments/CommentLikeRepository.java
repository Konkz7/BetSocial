package com.example.World.Comments;



import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;


public interface CommentLikeRepository extends ListCrudRepository<Commentlike_,Long> {


    @Query("SELECT * FROM CommentLike_ WHERE cid = :cid AND uid = :uid")
    Commentlike_ findByCommentAndUser(
            @Param("cid") Long cid,
            @Param("uid") Long uid);
}
