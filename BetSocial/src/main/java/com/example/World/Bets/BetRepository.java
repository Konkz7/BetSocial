package com.example.World.Bets;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Optional;


public interface BetRepository extends ListCrudRepository<Bet_,Long> {

        @Query("SELECT * FROM Bet_ WHERE status = :status AND deleted_at IS NULL")
        List<Bet_> findByStatus(
                      @Param("status") Integer status);

        /**
         * Bets whose owner has declared an outcome and which are waiting on an
         * approver. Exactly the set /superusers/approval will accept, so the queue
         * cannot show something that would be refused on arrival.
         *
         * Oldest first: whoever has been waiting longest gets paid first.
         */
        @Query("""
        SELECT * FROM Bet_
        WHERE status = :status AND outcome IS NOT NULL AND deleted_at IS NULL
        ORDER BY ends_at ASC
        """)
        List<Bet_> findAwaitingApproval(@Param("status") Integer status);


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
                         @Param("decided_at") Long decided_at,
                         @Param("user_id") Long user_id);

        @Modifying
        @Transactional
        @Query("UPDATE Bet_ SET  deleted_at = :deleted_at WHERE bid = :id AND deleted_at IS NULL")
        int remove(
                @Param("id") Long id,
                @Param("deleted_at") Long deleted_at);

}

