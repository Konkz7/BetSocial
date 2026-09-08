-- message_.is_read stops being written.
--
-- Whether a message has been read is now derived from
-- groupuser_.last_read_timestamp: a message is read by someone if it was created
-- before the last time they opened that conversation. One rule that works for two
-- people or twenty, where a single boolean on the message could never express
-- "read by 3 of 5" - it recorded one recipient's state and had nothing to say
-- about anybody else's.
--
-- The column is NOT NULL with no default, so leaving it out of an insert would
-- fail outright. A default lets the application stop writing it while the column
-- is still there. It is not dropped here: dropping is irreversible, so it joins
-- group_.sort, groupuser_.other_uid, message_.recipient_id and group_.deleted_at
-- in the set to remove together once this has been exercised for real.
--
-- Existing values are left as they are. Nothing reads them any more, and the
-- timestamps that replace them are already being recorded - the client has been
-- calling update-timestamp on leaving a conversation all along, so the data to
-- derive from is present rather than starting from empty.

ALTER TABLE public.message_
    ALTER COLUMN is_read SET DEFAULT false;
