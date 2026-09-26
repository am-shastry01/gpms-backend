-- =====================================================================
-- The third signature
-- =====================================================================
--
-- A gate pass carries three blocks: employee, manager, admin. Until
-- now only two of them were tracked as signatures - the employee
-- block was simply whoever raised the pass, which worked while only
-- employees raised them.
--
-- Now all three roles raise passes, and the rule is the same
-- whoever does: the creator's own block is signed the moment the
-- pass exists, and the other two still have to be signed by someone
-- holding those roles. A manager-raised pass therefore waits on an
-- employee and an admin.
--
-- Nullable, like the other two: a pass that is still going round is
-- SUPPOSED to have blanks in it, and that is what the printed
-- document shows.

ALTER TABLE gate_pass_requests
    ADD COLUMN employee_approved_by_user_id UUID
        REFERENCES users (id),
    ADD COLUMN employee_approved_at TIMESTAMPTZ,
    ADD COLUMN employee_signature_id UUID
        REFERENCES digital_signatures (id);

COMMENT ON COLUMN gate_pass_requests.employee_approved_by_user_id IS
    'The employee who signed. On an employee-raised pass this is the '
    'requester, stamped at creation.';

-- Every pass already in the table was raised by an employee under
-- the old rule, so their block is signed by the requester at the
-- moment the pass was created. Back-filling keeps history honest
-- rather than showing historical passes as missing a signature they
-- effectively had.
UPDATE gate_pass_requests
SET employee_approved_by_user_id = requested_by_user_id,
    employee_approved_at = created_at
WHERE employee_approved_at IS NULL;

-- The signature queue asks "which passes is my block still blank
-- on", once per role.
CREATE INDEX idx_gate_pass_requests_employee_unsigned
    ON gate_pass_requests (warehouse_id)
    WHERE employee_approved_at IS NULL;
