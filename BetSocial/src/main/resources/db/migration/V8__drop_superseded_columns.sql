-- Drops the five columns the group-messaging work left behind.
--
-- Each was retired in its own change and deliberately left in place, because
-- dropping a column is irreversible and the replacement deserved to be exercised
-- for real first. All five have now been through a working app, so the schema can
-- catch up with the code - until it does, anyone reading the baseline sees a data
-- model that stopped being true several changes ago.
--
--   group_.sort            0 for a direct message, 1 for a group. A conversation
--                          is one thing now; whether it is direct is carried by
--                          whether it has a name.
--   group_.deleted_at      a group is only ever hard-deleted, so its absence is
--                          the whole signal. Membership is what soft-deletes.
--   groupuser_.other_uid   the counterparty of a direct message. The other people
--                          in any conversation are its other membership rows.
--   message_.recipient_id  who a message was for. A message goes to its
--                          conversation, and that reaches whoever is in it.
--   message_.is_read       whether a message had been read. That is a fact about
--                          each reader, held on their membership row as
--                          last_read_timestamp - one boolean here could never say
--                          "read by 3 of 5".
--
-- Nothing reads or writes any of them. Existing values are discarded with the
-- columns; none of it is recoverable, and none of it is used.

-- Depends on group_.deleted_at, so it has to go first to say so out loud.
-- Postgres would drop it along with the column regardless.
ALTER TABLE public.group_
    DROP CONSTRAINT chk_group_deleted_after_created;

ALTER TABLE public.group_
    DROP COLUMN sort,
    DROP COLUMN deleted_at;

ALTER TABLE public.groupuser_
    DROP COLUMN other_uid;

-- Takes fk_recipient_id and midx_recipient_id with it: both exist only for this
-- column, and Postgres removes them with it.
ALTER TABLE public.message_
    DROP COLUMN recipient_id;

ALTER TABLE public.message_
    DROP COLUMN is_read;

-- Deliberately untouched: notification_.is_read is a different column on a
-- different table, still written and still read, and idx_notifications_user_id
-- covers it.
