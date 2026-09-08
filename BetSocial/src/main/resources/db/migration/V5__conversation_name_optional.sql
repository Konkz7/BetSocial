-- Removes the last structural difference between a direct message and a group.
--
-- A conversation is now just a set of members. What used to be group_.sort - 0
-- for a direct message, 1 for a group - is carried by whether the conversation
-- has a name at all:
--
--   group_name IS NULL      a direct conversation between exactly two people,
--                           titled in the client from the other member
--   group_name IS NOT NULL  a named group
--
-- That is one field where there were two, and it is a field the client needs
-- regardless. It also retires the junk name createDMGroup used to invent to get
-- past the constraint below - the two uids concatenated, "1215", shown to nobody
-- and meaning nothing.
--
-- The distinction still has to exist because a two-person conversation is closed:
-- adding a third person starts a new group rather than pulling a stranger into a
-- private history. Deriving it beats storing it twice.

ALTER TABLE public.group_
    DROP CONSTRAINT chk_group_name;

-- 'Unamed' [sic] would otherwise be substituted for the NULL that now carries
-- meaning, making every direct conversation look like a badly named group.
ALTER TABLE public.group_
    ALTER COLUMN group_name DROP DEFAULT;

-- A name remains optional, but a blank one is still not a name.
ALTER TABLE public.group_
    ADD CONSTRAINT chk_group_name_not_blank
    CHECK (group_name IS NULL OR group_name <> '');

-- Left in place deliberately, not dropped here: group_.sort, groupuser_.other_uid,
-- message_.recipient_id and group_.deleted_at are all now unread and unwritten.
-- Dropping columns is irreversible, so they follow the same route as
-- message_.is_read - stop using them first, drop them together once the change
-- has been exercised for real.
