-- =====================================================================
-- One-time codes for the forgotten-password flow.
--
-- A row per code sent. The code itself is NOT stored: only a hash of
-- it, the same way a password is, because a table of live reset codes
-- in plain text is a table of live passwords.
--
-- Keyed on the email address rather than the user id on purpose. The
-- send step must not reveal whether an address has an account, so it
-- records the request either way and only decides whether to actually
-- send - and an address with no account therefore has no id to key on.
--
-- Deliberately NOT shaped like the rest of the schema: no created_by /
-- updated_by, no soft-delete columns. Those come from BaseEntity and
-- its auditing listener, which needs a signed-in user to attribute the
-- write to - and nobody is signed in when a password is forgotten. A
-- spent code is also genuinely rubbish afterwards, so it is deleted
-- rather than flagged.
-- =====================================================================

CREATE TABLE password_reset_otps (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Lower-cased by the application before it is written, so a lookup
    -- never has to care how the person typed their address.
    email            VARCHAR(255) NOT NULL,

    -- The delegating encoder's format, e.g. {bcrypt}$2a$10$...
    code_hash        VARCHAR(255) NOT NULL,

    expires_at       TIMESTAMPTZ  NOT NULL,

    -- Wrong guesses so far. The code dies at a handful, so a six digit
    -- number cannot be walked through.
    attempts         INTEGER      NOT NULL DEFAULT 0,

    -- Set the moment a code is accepted. A consumed code is spent even
    -- while it is still inside its expiry window.
    consumed_at      TIMESTAMPTZ,

    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- The verify step looks up the newest live code for one address, and
-- the send step clears the previous ones for it.
CREATE INDEX idx_password_reset_otps_email
    ON password_reset_otps (email, created_at DESC);
