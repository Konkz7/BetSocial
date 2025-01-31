package com.example.World.Bets;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface BetRepository extends ListCrudRepository<Bet_,Long> {

        @Modifying
        @Transactional
        @Query("UPDATE Bet SET result = :result, amount_for = :amount_for , amount_against = :amount_against, status = :status WHERE bid = :id AND deleted_at IS NULL")
        int updateBet(@Param("id") Integer id,
                         @Param("outcome") Boolean outcome,
                         @Param("amount_for") Float amount_for,
                         @Param("amount_against") Float amount_against,
                         @Param("status") Integer status);



        @Query("SELECT * FROM Bet_ WHERE status = :status AND deleted_at IS NULL")
        List<Bet_> findByStatus(
                      @Param("status") Integer status);


        @Query("SELECT * FROM Bet_ WHERE tid = :tid AND deleted_at IS NULL")
        List<Bet_> findByThread(
                @Param("tid") Long tid);


        @Modifying
        @Transactional
        @Query("UPDATE Bet_ SET status = :status WHERE bid = :id AND deleted_at IS NULL")
        int updateStatus(@Param("id") Long id,
                      @Param("status") Integer status);

        @Modifying
        @Transactional
        @Query("UPDATE Bet_ SET outcome = :outcome WHERE bid = :id AND deleted_at IS NULL")
        int updateOutcome(@Param("id") Long id,
                         @Param("outcome") Boolean outcome);

        @Modifying
        @Transactional
        @Query("INSERT INTO Decision_Log (bet_id , reason , decision , decided_at , user_id) VALUES (:bet_id , :reason , :decision , :decided_at , :user_id)")
        int makeDecision(
                         @Param("bet_id") Long bet_id,
                         @Param("reason") String reason,
                         @Param("decision") Boolean decision,
                         @Param("decided_at") LocalDateTime decided_at,
                         @Param("user_id") Long user_id);

        @Modifying
        @Transactional
        @Query("UPDATE Bet_ SET  deleted_at = :deleted_at WHERE bid = :id AND deleted_at IS NULL")
        int remove(
                @Param("id") Long id,
                @Param("deleted_at") LocalDateTime deleted_at);

}

