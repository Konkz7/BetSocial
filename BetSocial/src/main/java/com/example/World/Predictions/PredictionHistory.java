package com.example.World.Predictions;

/**
 * One of your wagers, with enough around it to be worth reading.
 *
 * A Prediction_ on its own is a bid, a side and two numbers - it cannot say what
 * was being predicted, which is the one thing a list of your own bets has to
 * say. So the thread's title and the bet's description come with it.
 *
 * amount_won is already the net result and needs no arithmetic on top:
 *
 *   null      still running, or closed and waiting on its owner to declare
 *   negative  lost, and this is what it cost
 *   zero      refunded - nobody backed the outcome, so every stake went back
 *   positive  won, and this is the profit on top of the stake returned
 *
 * That last one is worth stating because it is the one people get wrong: a 100
 * stake that wins 40 shows 40 here, and the wallet received 140.
 */
public record PredictionHistory(
        Long pid,
        Long bid,
        Long tid,
        String thread_title,
        String bet_description,
        /** true for, false against. */
        Boolean prediction,
        Long amount_bet,
        Long amount_won,
        Integer bet_status,
        Long ends_at,
        Long created_at
) {
}
