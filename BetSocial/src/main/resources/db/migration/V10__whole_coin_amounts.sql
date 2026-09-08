-- Money becomes whole coins everywhere.
--
-- These were all `real` - 32-bit floats, which cannot represent most decimal
-- amounts exactly and are the wrong type for counting anything. A pool built by
-- repeatedly adding floats also drifts: add 0.1 enough times and the total is not
-- what you added. That is tolerable for a number nobody spends and unacceptable
-- for one that decides who gets paid.
--
-- The ledger already counts in whole BIGINT coins. These have to agree with it or
-- a stake of 10.5 could be taken from a wallet that can only hold whole numbers.
--
-- ROUND before casting rather than truncating: existing values are all zero on any
-- real database, but silently turning 9.9 into 9 is the wrong default for money.

ALTER TABLE public.bet_
    ALTER COLUMN amount_for   TYPE bigint USING ROUND(amount_for)::bigint,
    ALTER COLUMN amount_against TYPE bigint USING ROUND(amount_against)::bigint,
    ALTER COLUMN max_amount   TYPE bigint USING ROUND(max_amount)::bigint,
    ALTER COLUMN min_amount   TYPE bigint USING ROUND(min_amount)::bigint;

-- A prediction without a stake is not a prediction now that staking costs
-- something, so the column stops being nullable. Existing nulls become zero
-- first; there are none on any database that has run, since nothing has ever
-- created a prediction.
UPDATE public.prediction_ SET amount_bet = 0 WHERE amount_bet IS NULL;

ALTER TABLE public.prediction_
    ALTER COLUMN amount_bet TYPE bigint USING ROUND(amount_bet)::bigint,
    ALTER COLUMN amount_won TYPE bigint USING ROUND(amount_won)::bigint;

ALTER TABLE public.prediction_
    ALTER COLUMN amount_bet SET NOT NULL;

-- A stake has to be worth something, and a pool cannot owe coins.
ALTER TABLE public.prediction_
    ADD CONSTRAINT chk_prediction_amount_positive CHECK (amount_bet > 0);

ALTER TABLE public.bet_
    ADD CONSTRAINT chk_bet_pools_not_negative
    CHECK (amount_for >= 0 AND amount_against >= 0);
