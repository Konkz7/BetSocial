-- Backfill missed by V5.
--
-- V5 made group_name optional so that a direct conversation could go unnamed and
-- be titled from whoever else is in it. It did not touch the conversations that
-- already existed. Those still carry the name createDMGroup used to invent - the
-- two participants' uids concatenated, so users 8 and 5 gave "85" - and a name is
-- now precisely what marks a conversation as a group rather than a direct one.
--
-- The visible result was a conversation listed as "85" instead of the person you
-- were talking to, and lifecycle operations treating a private chat as a group.
--
-- The predicate is deliberately narrow. Matching on sort = 0 alone would be
-- wrong: sort is no longer written, so every conversation created since V5 -
-- groups included - has taken its database default of 0, and this would have
-- wiped their real names. A name is only cleared when it is exactly the
-- concatenation of two of that conversation's own members' uids, which is what
-- createDMGroup produced and what nobody would choose deliberately.

UPDATE public.group_ g
SET group_name = NULL
WHERE g.group_name IS NOT NULL
  AND EXISTS (
    SELECT 1
    FROM public.groupuser_ a
    JOIN public.groupuser_ b ON b.gid = a.gid AND b.uid <> a.uid
    WHERE a.gid = g.gid
      AND g.group_name = a.uid::text || b.uid::text
  );
