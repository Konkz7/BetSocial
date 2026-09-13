package com.example.World.Predictions;



import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;


import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface PredictionRepository extends ListCrudRepository<Prediction_,Long> {
    @Modifying
    @Transactional
    @Query("UPDATE Prediction_ SET prediction = :prediction, amount_bet = :amount WHERE pid = :id AND deleted_at IS NULL")
    int updatePrediction(@Param("id") Long id,
                     @Param("prediction") Boolean prediction,
                     @Param("amount") Long amount);

    @Query("SELECT * FROM Prediction_ WHERE bid = :id AND deleted_at IS NULL")
    List<Prediction_> findByBid(
                  @Param("id") Long id);

    @Query("SELECT * FROM Prediction_ WHERE uid = :uid AND bid = :bid AND deleted_at IS NULL")
    Optional<Prediction_> findByUidAndBid(
            @Param("uid") Long uid,
            @Param("bid") Long bid

    );

    /** Everything one person has staked on, newest first. */
    @Query("SELECT * FROM Prediction_ WHERE uid = :uid AND deleted_at IS NULL ORDER BY created_at DESC")
    List<Prediction_> findByUid(@Param("uid") Long uid);

    /**
     * Total staked and distinct bettors, for a whole page of threads at once.
     *
     * One query for the page rather than one per card. The comment counts are
     * gathered the same way and for the same reason - FeedQueryCountTest pins
     * that the feed's query count does not grow with the number of threads, and
     * a lookup per card is exactly what it exists to catch.
     *
     * A thread nobody has staked on is absent rather than zero; the caller
     * defaults, which is cheaper than a LEFT JOIN across three tables to
     * manufacture rows of nothing.
     *
     * Summed over predictions rather than over bet.amount_for + amount_against.
     * The two should agree, but the ledger is the record of what people actually
     * staked, and a bet's running totals are a denormalisation of it - if they
     * ever disagree, this is the one that is true.
     */
    @Query("""
    SELECT b.tid AS tid,
           COALESCE(SUM(p.amount_bet), 0) AS pool,
           COUNT(DISTINCT p.uid) AS bettors
    FROM Prediction_ p
    JOIN Bet_ b ON b.bid = p.bid
    WHERE b.tid IN (:tids)
      AND p.deleted_at IS NULL
      AND b.deleted_at IS NULL
    GROUP BY b.tid
    """)
    List<ThreadStakeSummary> stakesByThreadIds(@Param("tids") Collection<Long> tids);

    /**
     * How many people took each side, for every bet on one thread.
     *
     * FILTER rather than two queries or two passes: one row per bet, both sides
     * counted in the same scan.
     */
    @Query("""
    SELECT p.bid AS bid,
           COUNT(*) FILTER (WHERE p.prediction = true)  AS people_for,
           COUNT(*) FILTER (WHERE p.prediction = false) AS people_against
    FROM Prediction_ p
    JOIN Bet_ b ON b.bid = p.bid
    WHERE b.tid = :tid
      AND p.deleted_at IS NULL
      AND b.deleted_at IS NULL
    GROUP BY p.bid
    """)
    List<BetSideCount> sideCountsByThread(@Param("tid") Long tid);

    @Modifying
    @Transactional
    @Query("UPDATE Prediction_ SET  amount_won = :amount WHERE pid = :id AND deleted_at IS NULL")
    int updateAmountWon(@Param("id") Long id,
                         @Param("amount") Long amount);

    @Modifying
    @Transactional
    @Query("UPDATE Bet_ SET  amount_for = :amount + amount_for WHERE bid = :id AND deleted_at IS NULL")
    int updateAmountFor(@Param("id") Long id,
                        @Param("amount") Long amount);

    @Modifying
    @Transactional
    @Query("UPDATE Bet_ SET  amount_against = :amount + amount_against WHERE bid = :id AND deleted_at IS NULL")
    int updateAmountAgainst(@Param("id") Long id,
                        @Param("amount") Long amount);


    /**
     * Soft-deletes a prediction.
     *
     * This previously read "UPDATE Bet_ ... WHERE bid = :id" - copy-pasted from
     * BetRepository.remove - so removing a prediction soft-deleted the unrelated
     * bet whose bid happened to equal the prediction's pid, and left the
     * prediction itself in place.
     */
    @Modifying
    @Transactional
    @Query("UPDATE Prediction_ SET  deleted_at = :deleted_at WHERE pid = :id AND deleted_at IS NULL")
    int remove(
            @Param("id") Long id,
            @Param("deleted_at") Long deleted_at);

}
