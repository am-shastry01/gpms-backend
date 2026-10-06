-- ---------------------------------------------------------------
-- MULTI-COMPANY
--
-- Until now the system held one company: a singleton company_profile
-- row, warehouses that belonged to nobody in particular, and an admin
-- who could see every warehouse there was. One deployment now serves
-- several companies, so a company becomes a real row and everything
-- else hangs off it.
--
-- The existing data is folded into the first company rather than
-- discarded - the warehouse, its people and their gate passes all
-- carry on as company number one.
-- ---------------------------------------------------------------

CREATE TABLE IF NOT EXISTS companies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    name VARCHAR(200) NOT NULL,
    gst_number VARCHAR(30),

    phone_number VARCHAR(40),
    email VARCHAR(150),
    website VARCHAR(200),

    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(120),
    state VARCHAR(120),
    postal_code VARCHAR(20),

    -- Printed at the head of every gate pass this company issues.
    logo_content_type VARCHAR(120),
    logo_data BYTEA,

    -- A company switched off cannot sign in at all. Kept rather than
    -- deleted, because its gate passes are documents.
    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ
);


-- The singleton profile becomes company number one, so nothing that
-- is already printed on a pass changes.
INSERT INTO companies (
    name, gst_number, phone_number, email, website,
    address_line1, address_line2, city, state, postal_code,
    logo_content_type, logo_data, created_by, updated_by
)
SELECT
    name, gst_number, phone_number, email, website,
    address_line1, address_line2, city, state, postal_code,
    logo_content_type, logo_data, 'migration', 'migration'
FROM company_profile
WHERE code = 'DEFAULT'
  AND NOT EXISTS (SELECT 1 FROM companies);

-- A deployment with no profile row at all still needs one company for
-- the existing warehouses to belong to.
INSERT INTO companies (name, created_by, updated_by)
SELECT 'Company 1', 'migration', 'migration'
WHERE NOT EXISTS (SELECT 1 FROM companies);


-- ---------------------------------------------------------------
-- WAREHOUSES BELONG TO A COMPANY
-- ---------------------------------------------------------------

ALTER TABLE warehouses
    ADD COLUMN IF NOT EXISTS company_id UUID REFERENCES companies (id);

UPDATE warehouses
SET company_id = (SELECT id FROM companies ORDER BY created_at, id LIMIT 1)
WHERE company_id IS NULL;

ALTER TABLE warehouses
    ALTER COLUMN company_id SET NOT NULL;

-- A warehouse code only has to be unique inside its own company. Two
-- clients both calling their main site WH-01 is normal.
ALTER TABLE warehouses
    DROP CONSTRAINT IF EXISTS warehouses_code_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_warehouse_company_code
    ON warehouses (company_id, upper(code))
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_warehouses_company
    ON warehouses (company_id, active, deleted);


-- ---------------------------------------------------------------
-- USERS BELONG TO A COMPANY
--
-- Usernames stay unique across the whole system, because signing in
-- names a username and nothing else - there is no company field on
-- the login screen and asking a driver's clerk to type one would be
-- worse than asking the super admin to pick a free name.
-- ---------------------------------------------------------------

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS company_id UUID REFERENCES companies (id);

UPDATE users u
SET company_id = w.company_id
FROM warehouses w
WHERE u.warehouse_id = w.id
  AND u.company_id IS NULL;

UPDATE users
SET company_id = (SELECT id FROM companies ORDER BY created_at, id LIMIT 1)
WHERE company_id IS NULL;

ALTER TABLE users
    ALTER COLUMN company_id SET NOT NULL;

-- An admin runs a whole company and is not posted to one site, so a
-- user may now have no warehouse.
ALTER TABLE users
    ALTER COLUMN warehouse_id DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_users_company
    ON users (company_id, active, deleted);
