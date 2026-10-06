/*
 * When this account's password last changed.
 *
 * Tokens are good for hours and there is no revocation list, so until
 * now changing a password did nothing to the token already on a phone -
 * a stolen handset kept working for the rest of the day even after the
 * owner had changed their password in a panic.
 *
 * A token issued before this moment is now refused. Existing rows get
 * the current time, which invalidates every token minted before this
 * migration ran: everyone signs in once more, which is the right thing
 * to happen the day revocation starts working.
 */

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMPTZ;

UPDATE users
SET password_changed_at = NOW()
WHERE password_changed_at IS NULL;
