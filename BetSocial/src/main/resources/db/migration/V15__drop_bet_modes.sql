-- king_mode and profit_mode, removed.
--
-- They were stored and read back and nothing ever branched on them. Not one line
-- of the payout, settlement or validation code asked what they were - they were
-- two booleans the client set, the server persisted, and the client rendered as
-- two controls nobody could explain.
--
-- Dropped rather than left in place. A column that means nothing is worse than
-- no column: the next person to read the schema has to work out whether it
-- matters, and the honest answer is only discoverable by grepping the whole
-- codebase and finding nothing.
--
-- No data is lost that anything was using. Every row has a value for these and
-- no behaviour has ever depended on one.
ALTER TABLE public.bet_
    DROP COLUMN king_mode,
    DROP COLUMN profit_mode;
