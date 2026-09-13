package com.example.World.Predictions;



import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;


import java.util.Optional;
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
     * The same predictions, with what they were about.
     *
     * A Prediction_ is a bid, a side and two numbers - it cannot say what was
     * being predicted, which is the one thing a list of your own bets has to
     * say. Joining here rather than fetching each bet and thread afterwards:
     * that is two N+1s over a list bounded only by how much somebody has staked.
     *
     * Deleted threads and bets are kept deliberately. Your stake happened and
     * your coins moved whether or not the thread still stands, and a wallet
     * history with rows missing is worse than one naming something gone.
     */
    @Query("""
    SELECT p.pid       AS pid,
           p.bid       AS bid,
           b.tid       AS tid,
           t.title     AS thread_title,
           b.description AS bet_description,
           p.prediction  AS prediction,
           p.amount_bet  AS amount_bet,
           p.amount_won  AS amount_won,
           b.status      AS bet_status,
           b.ends_at     AS ends_at,
           p.created_at  AS created_at
    FROM Prediction_ p
    JOIN Bet_ b    ON b.bid = p.bid
    JOIN Thread_ t ON t.tid = b.tid
    WHERE p.uid = :uid
      AND p.deleted_at IS NULL
    ORDER BY p.created_at DESC
    """)
    List<PredictionHistory> historyOf(@Param("uid") Long uid);

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
