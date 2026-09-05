package com.example.World.Groups;



import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


public interface GroupUserRepository extends ListCrudRepository<Groupuser_,Long> {


    @Query("SELECT * FROM Groupuser_ WHERE uid= :uid")
    List<Groupuser_> findByUid(@Param("uid") Long uid);

    @Query("SELECT * FROM Groupuser_ WHERE gid= :gid AND uid= :uid")
    Optional<Groupuser_> findByGidandUid(@Param("gid") Long gid, @Param("uid") Long uid);

    @Modifying
    @Transactional
    @Query("UPDATE Groupuser_ SET last_read_timestamp = :now WHERE guid = :guid")
    int updateReadTimestamp(@Param("guid") Long guid , @Param("now") Long now);

}
