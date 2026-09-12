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

        /**
         * The other half of that queue: closed bets still waiting on their own
         * owner to say what happened.
         *
         * Nothing asked this before, so a bet that closed simply stopped being
         * mentioned anywhere. It is not active, so it is off the staking screen;
         * it has no outcome, so findAwaitingApproval filters it out. The owner was
         * the only person who could move it on and had no way of knowing they
         * needed to.
         *
         * Joined to the thread because that is where ownership lives - a bet
         * belongs to whoever posted the thread it sits under.
         */
        @Query("""
        SELECT b.* FROM Bet_ b
        JOIN Thread_ t ON t.tid = b.tid
        WHERE b.status = :status
          AND b.outcome IS NULL
          AND b.deleted_at IS NULL
          AND t.uid = :uid
          AND t.deleted_at IS NULL
        ORDER BY b.ends_at ASC
        """)
        List<Bet_> findAwaitingOwnerDecision(@Param("uid") Long uid, @Param("status") Integer status);


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

