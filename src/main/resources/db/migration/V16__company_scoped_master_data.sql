-- ---------------------------------------------------------------
-- MASTER DATA BELONGS TO A COMPANY TOO
--
-- Vendors, drivers, vehicles, destinations, transport companies and
-- vehicle types were global, with globally unique names. On a
-- single-company deployment that was invisible. With several clients on
-- one database it is a leak and a collision at the same time:
--
--   - drivers.mobile_number was unique across everything, so the second
--     company to raise a pass for the same lorry driver would silently
--     resolve to the FIRST company's driver row - and then print that
--     company's spelling of his name on their gate pass.
--   - the same for vehicles.registration_number, which is how two
--     clients sharing one transporter would have started reading each
--     other's fleet.
--   - and every dropdown would have offered every other client's
--     transport companies.
--
-- So each of these tables gets an owner, and uniqueness moves from
-- "unique everywhere" to "unique within this company".
-- ---------------------------------------------------------------

DO $$
DECLARE
    first_company UUID;
    target TEXT;
BEGIN
    SELECT id INTO first_company FROM companies ORDER BY created_at, id LIMIT 1;

    FOREACH target IN ARRAY ARRAY[
        'vendors', 'drivers', 'vehicles',
        'destinations', 'transports', 'vehicle_types'
    ]
    LOOP
        EXECUTE format(
            'ALTER TABLE %I ADD COLUMN IF NOT EXISTS company_id UUID REFERENCES companies (id)',
            target);

        EXECUTE format(
            'UPDATE %I SET company_id = %L WHERE company_id IS NULL',
            target, first_company);

        EXECUTE format(
            'ALTER TABLE %I ALTER COLUMN company_id SET NOT NULL',
            target);

        EXECUTE format(
            'CREATE INDEX IF NOT EXISTS idx_%s_company ON %I (company_id, deleted)',
            target, target);
    END LOOP;
END $$;


-- ---------------------------------------------------------------
-- RETIRE DUPLICATES BEFORE THE NEW INDEXES GO ON
--
-- The old constraints were plain UNIQUE columns, which PostgreSQL
-- compares case-SENSITIVELY. The new indexes compare on upper(...),
-- because "Indore" and "INDORE" are one destination to the person
-- typing them and offering both in a dropdown is a bug.
--
-- That means rows which were legal yesterday collide today, and a real
-- database had exactly that: two live destinations, Indore and INDORE,
-- in the same company. CREATE UNIQUE INDEX then fails and takes the
-- whole deployment with it.
--
-- So the duplicates are retired first. In each group one row survives
-- and keeps its own spelling: an enabled row beats a disabled one, and
-- after that the oldest wins. The rest are marked deleted, which takes
-- them out of the dropdowns without destroying anything.
--
-- Destinations, transport companies and vehicle types are only ever
-- stored on a gate pass as text, so retiring one changes nothing that
-- has already been printed. Vendors, drivers and vehicles ARE pointed
-- at by foreign keys, so anything referring to a duplicate is moved
-- onto the survivor before that duplicate goes.
--
-- Every statement is re-runnable: on a database with no duplicates it
-- does nothing at all.
-- ---------------------------------------------------------------

-- ----- lists nothing points at: retire the duplicates outright -----

WITH ranked AS (
    SELECT id,
           row_number() OVER (
               PARTITION BY company_id, upper(name)
               ORDER BY active DESC, created_at, id
           ) AS position
    FROM destinations
    WHERE deleted = FALSE
)
UPDATE destinations target
SET deleted = TRUE,
    deleted_at = NOW()
FROM ranked
WHERE target.id = ranked.id
  AND ranked.position > 1;

WITH ranked AS (
    SELECT id,
           row_number() OVER (
               PARTITION BY company_id, upper(name)
               ORDER BY active DESC, created_at, id
           ) AS position
    FROM vehicle_types
    WHERE deleted = FALSE
)
UPDATE vehicle_types target
SET deleted = TRUE,
    deleted_at = NOW()
FROM ranked
WHERE target.id = ranked.id
  AND ranked.position > 1;

-- Transport companies are unique on both name and code, so both are
-- swept - name first, then whatever still collides on code.

WITH ranked AS (
    SELECT id,
           row_number() OVER (
               PARTITION BY company_id, upper(name)
               ORDER BY active DESC, created_at, id
           ) AS position
    FROM transports
    WHERE deleted = FALSE
)
UPDATE transports target
SET deleted = TRUE,
    deleted_at = NOW()
FROM ranked
WHERE target.id = ranked.id
  AND ranked.position > 1;

WITH ranked AS (
    SELECT id,
           row_number() OVER (
               PARTITION BY company_id, upper(code)
               ORDER BY active DESC, created_at, id
           ) AS position
    FROM transports
    WHERE deleted = FALSE
)
UPDATE transports target
SET deleted = TRUE,
    deleted_at = NOW()
FROM ranked
WHERE target.id = ranked.id
  AND ranked.position > 1;


-- ----- rows with foreign keys: move the references, then retire -----

DO $$
DECLARE
    duplicate RECORD;
BEGIN

    /*
     * Vendors, by name and then by code. A gate pass and a lorry both
     * point at a vendor, so both are moved onto the survivor first -
     * otherwise retiring the duplicate would leave a pass pointing at a
     * vendor that no longer appears anywhere.
     */
    FOR duplicate IN
        WITH ranked AS (
            SELECT id,
                   first_value(id) OVER (
                       PARTITION BY company_id, upper(name)
                       ORDER BY active DESC, created_at, id
                   ) AS survivor,
                   row_number() OVER (
                       PARTITION BY company_id, upper(name)
                       ORDER BY active DESC, created_at, id
                   ) AS position
            FROM vendors
            WHERE deleted = FALSE
        )
        SELECT id, survivor FROM ranked WHERE position > 1
    LOOP
        UPDATE gate_pass_requests SET vendor_id = duplicate.survivor
        WHERE vendor_id = duplicate.id;

        UPDATE vehicles SET vendor_id = duplicate.survivor
        WHERE vendor_id = duplicate.id;

        UPDATE vendors SET deleted = TRUE, deleted_at = NOW()
        WHERE id = duplicate.id;
    END LOOP;

    FOR duplicate IN
        WITH ranked AS (
            SELECT id,
                   first_value(id) OVER (
                       PARTITION BY company_id, upper(code)
                       ORDER BY active DESC, created_at, id
                   ) AS survivor,
                   row_number() OVER (
                       PARTITION BY company_id, upper(code)
                       ORDER BY active DESC, created_at, id
                   ) AS position
            FROM vendors
            WHERE deleted = FALSE
        )
        SELECT id, survivor FROM ranked WHERE position > 1
    LOOP
        UPDATE gate_pass_requests SET vendor_id = duplicate.survivor
        WHERE vendor_id = duplicate.id;

        UPDATE vehicles SET vendor_id = duplicate.survivor
        WHERE vendor_id = duplicate.id;

        UPDATE vendors SET deleted = TRUE, deleted_at = NOW()
        WHERE id = duplicate.id;
    END LOOP;

    /*
     * Vehicles, by registration number. mp09ab1234 and MP09AB1234 are
     * the same lorry.
     */
    FOR duplicate IN
        WITH ranked AS (
            SELECT id,
                   first_value(id) OVER (
                       PARTITION BY company_id, upper(registration_number)
                       ORDER BY active DESC, created_at, id
                   ) AS survivor,
                   row_number() OVER (
                       PARTITION BY company_id, upper(registration_number)
                       ORDER BY active DESC, created_at, id
                   ) AS position
            FROM vehicles
            WHERE deleted = FALSE
        )
        SELECT id, survivor FROM ranked WHERE position > 1
    LOOP
        UPDATE gate_pass_requests SET vehicle_id = duplicate.survivor
        WHERE vehicle_id = duplicate.id;

        UPDATE vehicles SET deleted = TRUE, deleted_at = NOW()
        WHERE id = duplicate.id;
    END LOOP;

    /*
     * Drivers, by mobile number first - that is the one the app looks a
     * driver up by - and then by licence number, which is compared
     * case-insensitively and may be null. A null licence is not a
     * duplicate of another null licence, which is why the index excludes
     * them and so does this.
     *
     * Only gate passes point at a driver; driver_notifications went with
     * the guard flow in V7.
     */
    FOR duplicate IN
        WITH ranked AS (
            SELECT id,
                   first_value(id) OVER (
                       PARTITION BY company_id, mobile_number
                       ORDER BY active DESC, created_at, id
                   ) AS survivor,
                   row_number() OVER (
                       PARTITION BY company_id, mobile_number
                       ORDER BY active DESC, created_at, id
                   ) AS position
            FROM drivers
            WHERE deleted = FALSE
        )
        SELECT id, survivor FROM ranked WHERE position > 1
    LOOP
        UPDATE gate_pass_requests SET driver_id = duplicate.survivor
        WHERE driver_id = duplicate.id;

        UPDATE drivers SET deleted = TRUE, deleted_at = NOW()
        WHERE id = duplicate.id;
    END LOOP;

    FOR duplicate IN
        WITH ranked AS (
            SELECT id,
                   first_value(id) OVER (
                       PARTITION BY company_id, upper(license_number)
                       ORDER BY active DESC, created_at, id
                   ) AS survivor,
                   row_number() OVER (
                       PARTITION BY company_id, upper(license_number)
                       ORDER BY active DESC, created_at, id
                   ) AS position
            FROM drivers
            WHERE deleted = FALSE
              AND license_number IS NOT NULL
        )
        SELECT id, survivor FROM ranked WHERE position > 1
    LOOP
        UPDATE gate_pass_requests SET driver_id = duplicate.survivor
        WHERE driver_id = duplicate.id;

        UPDATE drivers SET deleted = TRUE, deleted_at = NOW()
        WHERE id = duplicate.id;
    END LOOP;

END $$;


-- ---------------------------------------------------------------
-- UNIQUENESS, NOW PER COMPANY
--
-- Partial indexes on deleted = FALSE, so a name freed by a deletion
-- can be used again.
-- ---------------------------------------------------------------

ALTER TABLE vendors DROP CONSTRAINT IF EXISTS vendors_code_key;
ALTER TABLE vendors DROP CONSTRAINT IF EXISTS vendors_name_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_vendor_company_code
    ON vendors (company_id, upper(code)) WHERE deleted = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uq_vendor_company_name
    ON vendors (company_id, upper(name)) WHERE deleted = FALSE;

ALTER TABLE drivers DROP CONSTRAINT IF EXISTS drivers_mobile_number_key;
ALTER TABLE drivers DROP CONSTRAINT IF EXISTS drivers_license_number_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_driver_company_mobile
    ON drivers (company_id, mobile_number) WHERE deleted = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uq_driver_company_license
    ON drivers (company_id, upper(license_number))
    WHERE deleted = FALSE AND license_number IS NOT NULL;

ALTER TABLE vehicles DROP CONSTRAINT IF EXISTS vehicles_registration_number_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_vehicle_company_registration
    ON vehicles (company_id, upper(registration_number)) WHERE deleted = FALSE;

ALTER TABLE destinations DROP CONSTRAINT IF EXISTS destinations_name_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_destination_company_name
    ON destinations (company_id, upper(name)) WHERE deleted = FALSE;

ALTER TABLE transports DROP CONSTRAINT IF EXISTS transports_code_key;
ALTER TABLE transports DROP CONSTRAINT IF EXISTS transports_name_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_transport_company_code
    ON transports (company_id, upper(code)) WHERE deleted = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uq_transport_company_name
    ON transports (company_id, upper(name)) WHERE deleted = FALSE;

ALTER TABLE vehicle_types DROP CONSTRAINT IF EXISTS vehicle_types_name_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_vehicle_type_company_name
    ON vehicle_types (company_id, upper(name)) WHERE deleted = FALSE;
