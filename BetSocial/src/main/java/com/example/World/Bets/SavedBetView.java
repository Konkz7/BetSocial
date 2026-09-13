package com.example.World.Bets;

/**
 * A bet somebody bookmarked, with enough around it to find again.
 *
 * The bookmarking has worked for as long as the thread screen has had the icon,
 * and the row in Settings that was meant to show the results has never done
 * anything - there was a toggle and a per-bet lookup, and no way to ask for the
 * list.
 *
 * A Betsave_ is two ids. On its own it cannot say what was saved, which is the
 * only thing a list of saved bets is for, so the bet and its thread come with it.
 */
public record SavedBetView(
        Long bsid,
        Long bid,
        Long tid,
        String thread_title,
        String bet_description,
        Integer status,
        Long ends_at,
        /** null until somebody declares it; true means the "for" side was right. */
        Boolean outcome
) {
}
