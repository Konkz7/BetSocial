-- is_verified, removed, for the same reason as king_mode and profit_mode.
--
-- It was the third of three booleans on this table that were stored, drawn and
-- never read. Nothing in the payout, the settlement, the approval queue or the
-- validation ever asked whether a bet was verified - the client set it, the
-- server kept it, and the thread screen drew a shield.
--
-- A separate migration rather than an edit to V15, which dropped the other two.
-- V15 is not merged and has been applied nowhere, so editing it would be tidier
-- and would also be the habit that eventually rewrites a migration somebody has
-- already run. Flyway records a checksum; the cost of one extra file is nothing
-- against the cost of that.
ALTER TABLE public.bet_
    DROP COLUMN is_verified;
