package com.example.World.Predictions;

/**
 * What has been staked on one thread, and by how many people.
 *
 * The feed card showed "$2.5K" and "18" - written into the component, identical
 * on every thread, and wrong on all of them. These are the two numbers it was
 * pretending to have.
 *
 * Counted over a whole page in one query, the same way comment counts are: a
 * lookup per card would be an N+1 that grows with the page size, which
 * FeedQueryCountTest exists to prevent.
 */
public record ThreadStakeSummary(Long tid, Long pool, Long bettors) {
}
