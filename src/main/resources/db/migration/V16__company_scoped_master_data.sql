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
