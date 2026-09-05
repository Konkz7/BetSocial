package com.example.World.Bets;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


public interface BetSaveRepository extends ListCrudRepository<Betsave_,Long> {

    @Query("SELECT * FROM BetSave_ WHERE bid = :bid AND uid = :uid")
    Betsave_ findByBetAndUser(
            @Param("bid") Long bid,
            @Param("uid") Long uid);

}

