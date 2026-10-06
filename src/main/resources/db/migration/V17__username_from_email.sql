/*
 * The sign-in name is the email address.
 *
 * A person's identity is their work email, which is already unique and
 * already carries the company in it - ravi@acme.com and ravi@beta.com
 * are different people without anybody having to invent ravi.acme. So
 * the username column stops being a thing an admin types and becomes a
 * copy of the email, which is what keeps it unique.
 *
 * The column stays: it is the subject of every JWT and the handle the
 * seeded accounts (admin, manager, employee) still sign in under. It
 * only has to be wide enough to hold an email now.
 */

ALTER TABLE users
    ALTER COLUMN username TYPE VARCHAR(150);
