-- ---------------------------------------------------------------
-- GATE PASS NUMBERS, PER COMPANY
--
-- Both numbers came from one database-wide sequence, so the second
-- client onboarded would have seen their very first gate pass numbered
-- GP-2026-000457 - continuing somebody else's count, and quietly
-- telling them how much business the first client does.
--
-- Each company now counts for itself, and carries a short code that
-- goes at the front of the number so two companies' documents can never
-- be mistaken for one another.
-- ---------------------------------------------------------------

ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS code VARCHAR(12);


-- Existing companies get a code derived from their name: letters and
-- digits only, first four characters, uppercased - with a counter
-- appended if that collides with one already taken.
DO $$
DECLARE
    row_company RECORD;
    base TEXT;
    candidate TEXT;
    suffix INT;
BEGIN
    FOR row_company IN SELECT id, name FROM companies WHERE code IS NULL ORDER BY created_at, id
    LOOP
        base := upper(regexp_replace(coalesce(row_company.name, 'CO'), '[^A-Za-z0-9]', '', 'g'));

        IF base = '' THEN
            base := 'CO';
        END IF;

        base := left(base, 4);
        candidate := base;
        suffix := 1;

        WHILE EXISTS (SELECT 1 FROM companies WHERE code = candidate) LOOP
            suffix := suffix + 1;
            candidate := left(base, 3) || suffix::TEXT;
        END LOOP;

        UPDATE companies SET code = candidate WHERE id = row_company.id;
    END LOOP;
END $$;

ALTER TABLE companies
    ALTER COLUMN code SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_company_code
    ON companies (upper(code))
    WHERE deleted = FALSE;


-- ---------------------------------------------------------------
-- THE COUNTERS
--
-- One row per company, per kind of document, per year. Keyed that way
-- because the year is printed in the number, so a counter that did not
-- reset with it would make the year decorative.
--
-- Incremented by a single INSERT ... ON CONFLICT ... RETURNING, which
-- is atomic on its own - no SELECT FOR UPDATE, and two requests landing
-- together cannot be handed the same number.
-- ---------------------------------------------------------------

CREATE TABLE IF NOT EXISTS company_document_counters (
    company_id UUID NOT NULL REFERENCES companies (id),

    -- 'REQUEST' or 'GATE_PASS'
    kind VARCHAR(20) NOT NULL,

    year INT NOT NULL,

    -- The number most recently handed out.
    last_value BIGINT NOT NULL DEFAULT 0,

    PRIMARY KEY (company_id, kind, year)
);
