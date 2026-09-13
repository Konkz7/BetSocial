-- Whether this account wants push notifications on its device.
--
-- The Settings toggle existed and did nothing: there was no column behind it, so
-- it could be moved and nothing changed. This is that column.
--
-- Default true, because that is what everybody currently gets. A default of
-- false would silently switch push off for every existing account on deploy,
-- which is a change nobody asked for dressed up as a migration.
--
-- NOT NULL so there is no third state. "Null" would end up meaning "probably
-- yes" at every call site, and one of them would eventually read it as no.
ALTER TABLE public.user_
    ADD COLUMN push_enabled boolean NOT NULL DEFAULT true;

COMMENT ON COLUMN public.user_.push_enabled IS
    'Push notifications only. Notification_ rows are still written when this is '
    'false, so the in-app activity list keeps working - switching off push is not '
    'asking to stop being notified, it is asking the phone to stay quiet.';
