-- Getting back into an account after forgetting the password.
--
-- There was no way to. AuthService.sendPasswordResetEmail existed and had never
-- been called, and calling it would not have helped: it asks Firebase Auth to
-- reset a Firebase password, while sign-in goes through DaoAuthenticationProvider
-- against the BCrypt hash in this table. Somebody would have followed the link,
-- set a new password, and still been locked out.
--
-- Separate columns from verification_token on purpose. That token proves an email
-- address is real; this one authorises changing a password. Sharing one column
-- would let a verification link be used to take over an account.

ALTER TABLE public.user_
    -- A SHA-256 of the token, not the token. A leaked database should not hand
    -- somebody a working reset link for every account that has asked for one.
    ADD COLUMN password_reset_token character varying(64),

    -- Milliseconds, like every other timestamp here. A reset link that never
    -- expires is a permanent second password sitting in somebody's inbox.
    ADD COLUMN password_reset_expires_at bigint;

-- Unique so two accounts cannot end up with the same token, partial so the
-- overwhelming majority of rows - every account not mid-reset - stay out of it.
CREATE UNIQUE INDEX idx_user_password_reset_token
    ON public.user_ (password_reset_token)
    WHERE password_reset_token IS NOT NULL;

-- Both columns are set together and cleared together. A token with no expiry
-- would never time out; an expiry with no token is meaningless.
ALTER TABLE public.user_
    ADD CONSTRAINT chk_password_reset_pair_complete
    CHECK ((password_reset_token IS NULL AND password_reset_expires_at IS NULL)
        OR (password_reset_token IS NOT NULL AND password_reset_expires_at IS NOT NULL));
