package com.example.World.Threads;


import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ThreadLikeRepository extends ListCrudRepository<Threadlike_, Long> {

    @Query("SELECT * FROM ThreadLike_ WHERE tid = :tid AND uid = :uid")
    Threadlike_ findByThreadAndUser(
            @Param("tid") Long tid,
            @Param("uid") Long uid);
}
