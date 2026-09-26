-- ---------------------------------------------------------------
-- V8 - the gate pass exists from the moment it is requested
--
-- The old flow held the gate pass hostage to approval: a request
-- sat in PENDING with no number, and only once a manager approved
-- did it become a real pass. The client's flow is the other way
-- round. Whoever raises the request - employee, manager or admin -
-- gets a gate pass immediately, and approval is a signature added
-- to a document that already exists.
--
-- That means two things here:
--
--   1. Approval is now recorded PER ROLE. A manager signing and an
--      admin signing are separate events with separate dates, and
--      either can happen without the other. One approved_by column
--      cannot express that.
--
--   2. Requests that already exist need a number, or they would be
--      the only ones in the system without a printable pass.
--
-- The existing approved_by_user_id / approval_time pair is kept and
-- goes on recording whichever approval happened FIRST, so reports
-- and the approval trail carry on working untouched.
-- ---------------------------------------------------------------

ALTER TABLE gate_pass_requests
    ADD COLUMN IF NOT EXISTS manager_approved_by_user_id UUID REFERENCES users (id),
    ADD COLUMN IF NOT EXISTS manager_approved_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS admin_approved_by_user_id UUID REFERENCES users (id),
    ADD COLUMN IF NOT EXISTS admin_approved_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_gate_pass_manager_approved_at
    ON gate_pass_requests (manager_approved_at);
CREATE INDEX IF NOT EXISTS idx_gate_pass_admin_approved_at
    ON gate_pass_requests (admin_approved_at);


-- ---------------------------------------------------------------
-- Backfill: split the single historical approval by the approver's
-- role, so an old pass shows its signature in the right block
-- rather than in neither.
-- ---------------------------------------------------------------
UPDATE gate_pass_requests g
SET admin_approved_by_user_id = g.approved_by_user_id,
    admin_approved_at         = g.approval_time
WHERE g.approved_by_user_id IS NOT NULL
  AND g.admin_approved_at IS NULL
  AND EXISTS (
      SELECT 1
      FROM user_roles ur
      JOIN roles r ON r.id = ur.role_id
      WHERE ur.user_id = g.approved_by_user_id
        AND r.code = 'ADMIN'
  );

UPDATE gate_pass_requests g
SET manager_approved_by_user_id = g.approved_by_user_id,
    manager_approved_at         = g.approval_time
WHERE g.approved_by_user_id IS NOT NULL
  AND g.manager_approved_at IS NULL
  AND g.admin_approved_at IS NULL
  AND EXISTS (
      SELECT 1
      FROM user_roles ur
      JOIN roles r ON r.id = ur.role_id
      WHERE ur.user_id = g.approved_by_user_id
        AND r.code = 'MANAGER'
  );


-- ---------------------------------------------------------------
-- Backfill: every live request gets a gate pass number.
--
-- Drawn from the same sequence the application uses and formatted
-- the same way (GP-<year>-<6 digits>), so backfilled numbers are
-- indistinguishable from newly issued ones and cannot collide with
-- them. Rejected and cancelled requests are skipped - there is no
-- pass to print for a request that went nowhere.
-- ---------------------------------------------------------------
UPDATE gate_pass_requests
SET gate_pass_number = 'GP-'
    || EXTRACT(YEAR FROM NOW())::int
    || '-'
    || LPAD(NEXTVAL('gate_pass_number_seq')::text, 6, '0')
WHERE gate_pass_number IS NULL
  AND status NOT IN ('REJECTED', 'CANCELLED');
