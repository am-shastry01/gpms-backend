-- ---------------------------------------------------------------
-- V6 - managed master data
--
-- Destination, transport company and vehicle type were all free
-- text typed into the request form. That is why the same place
-- appears three different ways in the reports, and why nothing can
-- be retired from circulation without editing history.
--
-- Each becomes a managed list that an admin can add to, rename,
-- and disable. Disabling takes an entry out of the dropdowns
-- WITHOUT touching the gate passes that already reference it -
-- which is the whole reason these are soft-deleted rows rather
-- than strings.
--
-- The seeds below lift whatever is already in the database so no
-- existing request is left pointing at a value that is not on the
-- list.
-- ---------------------------------------------------------------

CREATE TABLE IF NOT EXISTS destinations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS transports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL UNIQUE,
    contact_person VARCHAR(150),
    phone_number VARCHAR(30),
    email VARCHAR(150),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS vehicle_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ
);

-- Dropdowns read active rows constantly; everything else is rare.
CREATE INDEX IF NOT EXISTS idx_destinations_active ON destinations (active) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_transports_active ON transports (active) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_vehicle_types_active ON vehicle_types (active) WHERE deleted = FALSE;


-- ---------------------------------------------------------------
-- Seed from what already exists
-- ---------------------------------------------------------------

-- Destinations previously typed into gate_pass_requests.destination.
INSERT INTO destinations (name, created_by, updated_by)
SELECT DISTINCT TRIM(destination), 'migration', 'migration'
FROM gate_pass_requests
WHERE destination IS NOT NULL
  AND TRIM(destination) <> ''
ON CONFLICT (name) DO NOTHING;

-- Vehicle types previously typed into vehicles.vehicle_type.
INSERT INTO vehicle_types (name, created_by, updated_by)
SELECT DISTINCT TRIM(vehicle_type), 'migration', 'migration'
FROM vehicles
WHERE vehicle_type IS NOT NULL
  AND TRIM(vehicle_type) <> ''
ON CONFLICT (name) DO NOTHING;

-- A starter set, so a brand new database is not staring at an
-- empty dropdown on day one.
INSERT INTO vehicle_types (name, created_by, updated_by)
VALUES
    ('Truck', 'system', 'system'),
    ('Tempo', 'system', 'system'),
    ('Container', 'system', 'system'),
    ('Trailer', 'system', 'system'),
    ('Pickup', 'system', 'system')
ON CONFLICT (name) DO NOTHING;

-- Existing vendors are the closest thing to a transport list the
-- database has, so they are copied across as a starting point. The
-- vendors table itself is left exactly as it was.
INSERT INTO transports (code, name, contact_person, phone_number, email, active, created_by, updated_by)
SELECT code, name, contact_person, phone_number, email, active, 'migration', 'migration'
FROM vendors
WHERE deleted = FALSE
ON CONFLICT (code) DO NOTHING;
