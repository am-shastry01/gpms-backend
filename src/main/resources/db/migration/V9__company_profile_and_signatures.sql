-- ---------------------------------------------------------------
-- V9 - what the printed gate pass needs
--
-- Two things the PDF cannot be produced without:
--
--   1. WHOSE pass it is - the company name, address and logo that
--      go at the head of the document.
--
--   2. The approvers' signatures, as images.
--
-- The signature design is the part worth reading carefully.
--
-- Signatures are stored as IMMUTABLE VERSIONS, not as one editable
-- row per user. Uploading a new signature deactivates the previous
-- row and inserts a new one; nothing is ever overwritten.
--
-- That is not tidiness, it is the data integrity requirement. A
-- gate pass records the id of the exact signature row that was
-- applied when it was approved. If a manager later replaces their
-- signature, every pass they signed before still renders with the
-- signature that was actually on it at the time. Storing one
-- mutable signature per user would silently rewrite the history of
-- every document they had ever approved.
--
-- Images live in the database as bytea rather than in MinIO. They
-- are a few kilobytes each, they must survive wherever the app is
-- deployed, and the PDF needs them synchronously while rendering -
-- an object store buys nothing here and adds a failure mode.
-- ---------------------------------------------------------------

CREATE TABLE IF NOT EXISTS company_profile (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Singleton guard. There is one company, and a unique constant
    -- is what stops a second profile quietly appearing and the PDF
    -- picking whichever row came back first.
    code VARCHAR(20) NOT NULL UNIQUE DEFAULT 'DEFAULT',

    name VARCHAR(200) NOT NULL,
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(120),
    state VARCHAR(120),
    postal_code VARCHAR(20),
    phone_number VARCHAR(40),
    email VARCHAR(150),
    gst_number VARCHAR(30),

    logo_content_type VARCHAR(120),
    logo_data BYTEA,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ
);

INSERT INTO company_profile (code, name, created_by, updated_by)
VALUES ('DEFAULT', 'Your Company Name', 'migration', 'migration')
ON CONFLICT (code) DO NOTHING;


CREATE TABLE IF NOT EXISTS digital_signatures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID NOT NULL REFERENCES users (id),

    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    image_data BYTEA NOT NULL,
    size_bytes BIGINT NOT NULL,

    -- FALSE once superseded by a newer upload. The row stays so the
    -- gate passes that point at it still render.
    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ
);

-- At most one CURRENT signature per person. A partial unique index
-- rather than a plain one, because the superseded rows are all
-- allowed to share a user.
CREATE UNIQUE INDEX IF NOT EXISTS idx_digital_signature_current
    ON digital_signatures (user_id)
    WHERE active = TRUE AND deleted = FALSE;


-- ---------------------------------------------------------------
-- Which signature was actually applied, per approval
--
-- Nullable on purpose: an approver with no signature uploaded can
-- still approve. The pass then prints their name and date over a
-- blank signature line rather than refusing the approval outright.
-- ---------------------------------------------------------------
ALTER TABLE gate_pass_requests
    ADD COLUMN IF NOT EXISTS manager_signature_id UUID REFERENCES digital_signatures (id),
    ADD COLUMN IF NOT EXISTS admin_signature_id UUID REFERENCES digital_signatures (id);
