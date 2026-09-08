-- Membership becomes soft-deletable so that leaving or being removed from a
-- conversation is a fact the application can still see afterwards.
--
-- Without this, removal is indistinguishable from never having been a member:
-- the row is gone, so there is nothing to tell a user they were removed from a
-- group, and nothing to list the groups they used to be in.
--
-- The direction of travel is deliberately opposite for the two tables. A group_
-- row is only ever hard-deleted, and its foreign keys already cascade that to
-- groupuser_ and message_ (see V1). A groupuser_ row is only ever soft-deleted.
-- So the history survives for as long as the conversation itself does, and
-- deleting a conversation outright removes every trace of it in one step rather
-- than leaving rows pointing at a group that is no longer there.
--
-- Note this means a user can hold more than one membership row for the same
-- group over time - removed, then added back. Only one of them is ever active,
-- which is what every membership query filters on.

ALTER TABLE public.groupuser_
    ADD COLUMN deleted_at bigint;

-- Matches the invariant V3 established on every other soft-deletable table.
ALTER TABLE public.groupuser_
    ADD CONSTRAINT chk_groupuser_deleted_after_created
    CHECK (deleted_at IS NULL OR deleted_at >= created_at);

-- Membership is looked up by (gid, uid) and by uid on every conversation list,
-- and now always with a deleted_at predicate alongside.
CREATE INDEX idx_groupuser_active_by_uid
    ON public.groupuser_ (uid)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_groupuser_active_by_gid
    ON public.groupuser_ (gid)
    WHERE deleted_at IS NULL;
