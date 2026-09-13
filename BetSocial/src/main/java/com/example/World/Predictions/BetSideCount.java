package com.example.World.Predictions;

/**
 * How many people took each side of one bet.
 *
 * The thread page has a toggle between a people view and a money view, and both
 * read "for / against" - the words, on every bet. The money half was always
 * available (amount_for and amount_against are columns on Bet_ and already reach
 * the client); this is the half that was not.
 *
 * Separate from Bet_ rather than folded into it: Bet_ has seventeen fields, and a
 * view record repeating them all so two could be added is a thing that silently
 * goes out of date the first time a column is added.
 */
public record BetSideCount(Long bid, Long people_for, Long people_against) {
}
