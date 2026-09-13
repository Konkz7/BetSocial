package com.example.World.Bets;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


public interface BetSaveRepository extends ListCrudRepository<Betsave_,Long> {

    /**
     * Everything this person has bookmarked, with what it was.
     *
     * The saving has worked for as long as the thread screen has had the icon;
     * what was missing was any way to ask for the list, so the Settings row that
     * should have shown them has never done anything.
     *
     * Soft-deleted bets and threads are excluded here, unlike the prediction
     * history. A bookmark is a pointer to something to come back to, and a
     * pointer to something removed is not worth keeping - whereas a stake is a
     * record of coins that moved, which stays true whatever happened to the
     * thread afterwards.
     */
    @Query("""
    SELECT s.bsid   AS bsid,
           s.bid    AS bid,
           b.tid    AS tid,
           t.title  AS thread_title,
           b.description AS bet_description,
           b.status AS status,
           b.ends_at AS ends_at,
           b.outcome AS outcome
    FROM BetSave_ s
    JOIN Bet_ b    ON b.bid = s.bid
    JOIN Thread_ t ON t.tid = b.tid
    WHERE s.uid = :uid
      AND b.deleted_at IS NULL
      AND t.deleted_at IS NULL
    ORDER BY s.bsid DESC
    """)
    List<SavedBetView> savedBy(@Param("uid") Long uid);

    @Query("SELECT * FROM BetSave_ WHERE bid = :bid AND uid = :uid")
    Betsave_ findByBetAndUser(
            @Param("bid") Long bid,
            @Param("uid") Long uid);

}

