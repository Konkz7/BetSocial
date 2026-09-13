package com.example.World.Predictions;

/**
 * Somebody's track record, for a profile that is not your own.
 *
 * Counts and nothing else. A social prediction app is partly about who is worth
 * listening to, so a record earns its place on a profile - but how much somebody
 * stakes, and how much they have lost, is not the crowd's business.
 *
 * It is also the only shape that cannot leak. The full history carries thread
 * titles, and a prediction on a private thread would name a thread the viewer is
 * not allowed to know exists; a count says nothing about which threads they
 * were.
 *
 * <p>settled excludes two cases, and both matter:
 *
 * <ul>
 *   <li>still running, or waiting on a decision - not yet right or wrong
 *   <li>refunded, where nobody backed the outcome and every stake went back -
 *       that bet decided nothing, and counting it as a loss would punish
 *       somebody for a bet that never resolved
 * </ul>
 *
 * So the rate is correct out of settled, not correct out of total.
 */
public record PredictionRecord(Long total, Long settled, Long correct) {
}
