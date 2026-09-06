-- Enforces "a row cannot be deleted before it was created" in the database,
-- where an invariant belongs.
--
-- The entities used to assert this inside their created_at() accessor and throw
-- IllegalStateException. An accessor runs when Jackson serialises the record, so
-- a row that tripped the check became permanently unreadable - a 500 on every
-- request touching it, uncorrectable through the API. It also never prevented
-- the bad write in the first place; it only punished reads afterwards.
--
-- Note the comparison is >=, not >. The old code treated deleted_at == created_at
-- as invalid, but deleting something in the same millisecond it was created is
-- perfectly legitimate and happens readily in tests and fast client flows.
--
-- Verified against the development database before writing this: zero existing
-- rows violate the condition in any of these tables.

ALTER TABLE public.thread_
    ADD CONSTRAINT chk_thread_deleted_after_created
    CHECK (deleted_at IS NULL OR deleted_at >= created_at);

ALTER TABLE public.comment_
    ADD CONSTRAINT chk_comment_deleted_after_created
    CHECK (deleted_at IS NULL OR deleted_at >= created_at);

ALTER TABLE public.bet_
    ADD CONSTRAINT chk_bet_deleted_after_created
    CHECK (deleted_at IS NULL OR deleted_at >= created_at);

ALTER TABLE public.message_
    ADD CONSTRAINT chk_message_deleted_after_created
    CHECK (deleted_at IS NULL OR deleted_at >= created_at);

ALTER TABLE public.prediction_
    ADD CONSTRAINT chk_prediction_deleted_after_created
    CHECK (deleted_at IS NULL OR deleted_at >= created_at);

ALTER TABLE public.user_
    ADD CONSTRAINT chk_user_deleted_after_created
    CHECK (deleted_at IS NULL OR deleted_at >= created_at);

ALTER TABLE public.group_
    ADD CONSTRAINT chk_group_deleted_after_created
    CHECK (deleted_at IS NULL OR deleted_at >= created_at);

-- Deliberately NOT reinstated: the old Bet_ accessor also threw when
-- deleted_at <= ends_at ("cannot be ended after bet has been deleted"). That is
-- the normal case - ThreadService.removeThread cancels and soft-deletes bets
-- whose ends_at is still in the future - so it made every such bet unreadable.
