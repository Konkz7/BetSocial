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
    @Query("UPDATE Prediction_ SET prediction = :prediction, amount_bet = :amount WHERE pid = :id")
    int updatePrediction(@Param("id") Long id,
                     @Param("prediction") Boolean prediction,
                     @Param("amount") Float amount);

    @Query("SELECT * FROM Prediction_ WHERE bid = :id")
    List<Prediction_> findByBid(
                  @Param("id") Long id);

    @Query("SELECT * FROM Prediction_ WHERE uid = :uid AND bid = :bid")
    List<Prediction_> findByUidAndBid(
            @Param("uid") Long uid,
            @Param("bid") Long bid

    );

    @Modifying
    @Transactional
    @Query("UPDATE Prediction_ SET  amount_won = :amount WHERE pid = :id")
    int updateAmountWon(@Param("id") Long id,
                         @Param("amount") Float amount);

    @Modifying
    @Transactional
    @Query("UPDATE Bet_ SET  amount_for = :amount + amount_for WHERE bid = :id")
    int updateAmountFor(@Param("id") Long id,
                        @Param("amount") Float amount);

    @Modifying
    @Transactional
    @Query("UPDATE Bet_ SET  amount_against = :amount + amount_against WHERE bid = :id")
    int updateAmountAgainst(@Param("id") Long id,
                        @Param("amount") Float amount);

    @Modifying
    @Transactional
    @Query("UPDATE Bet_ SET  amount_against = 0 , amount_for = 0 WHERE bid = :id")
    int resetPredictionAmount(@Param("id") Long id);

}
