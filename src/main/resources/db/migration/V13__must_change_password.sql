/*
 * A password an admin typed for somebody else is a temporary one.
 *
 * The flag is raised when an admin creates an account and cleared
 * the moment that person sets a password of their own, so the app
 * can hold them on the "create a new password" screen until they
 * do. Existing accounts are left alone: they already chose their
 * own password, so the column defaults to false for them.
 */

ALTER TABLE users
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
