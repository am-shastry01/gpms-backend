-- ---------------------------------------------------------------
-- V7 - the guard is gone
--
-- The client's revised flow has two audiences: the people who raise
-- and approve gate passes, and nobody else. There is no security
-- role at the gate, no QR scan, and no SMS link to a driver's
-- phone - the gate pass is a document that gets printed and read.
--
-- So everything that only existed to serve that flow comes out:
-- the SECURITY role, the one-time driver tokens from V5, the
-- entry/exit timestamps from V4, and the QR payload.
--
-- Written to be safe to replay: every drop is guarded, and rows in
-- a status that no longer exists are moved to COMPLETED rather
-- than left pointing at a value the application cannot parse.
-- ---------------------------------------------------------------

-- ---------------------------------------------------------------
-- 1. Statuses that no longer exist in the enum
--
-- Must run BEFORE the columns are dropped: a request that had
-- already exited is finished, and COMPLETED is what finished means
-- now. Left alone, these rows would blow up on deserialisation the
-- first time anyone opened them.
-- ---------------------------------------------------------------
UPDATE gate_pass_requests
SET status = 'COMPLETED'
WHERE status IN ('ENTERED', 'EXITED');

-- ---------------------------------------------------------------
-- 2. The view has to go first
--
-- v_trucks_exited_today selects g.exit_time, so PostgreSQL will
-- refuse to drop that column while the view still depends on it.
-- ---------------------------------------------------------------
DROP VIEW IF EXISTS v_trucks_exited_today;

-- ---------------------------------------------------------------
-- 3. Driver notification log (V5)
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS driver_notifications;

-- ---------------------------------------------------------------
-- 4. Guard-owned columns and their indexes
-- ---------------------------------------------------------------
DROP INDEX IF EXISTS idx_gate_pass_access_token;
DROP INDEX IF EXISTS idx_gate_pass_entry_time;

ALTER TABLE gate_pass_requests
    DROP COLUMN IF EXISTS access_token,
    DROP COLUMN IF EXISTS token_issued_at,
    DROP COLUMN IF EXISTS token_expires_at,
    DROP COLUMN IF EXISTS token_consumed_at,
    DROP COLUMN IF EXISTS qr_content,
    DROP COLUMN IF EXISTS entry_time,
    DROP COLUMN IF EXISTS entered_by_user_id,
    DROP COLUMN IF EXISTS exit_time,
    DROP COLUMN IF EXISTS exited_by_user_id;

-- ---------------------------------------------------------------
-- 5. The SECURITY role itself
--
-- Role assignments are cleared first so the delete is not blocked
-- by the join table's foreign key. Any account that held ONLY the
-- security role is left with no role at all, which correctly means
-- it can sign in to nothing - it is deactivated rather than
-- deleted so the gate passes it touched still resolve a requester.
-- ---------------------------------------------------------------
UPDATE users
SET active = FALSE
WHERE id IN (
    SELECT ur.user_id
    FROM user_roles ur
    JOIN roles r ON r.id = ur.role_id
    WHERE r.code = 'SECURITY'
    GROUP BY ur.user_id
    HAVING COUNT(*) = 1
);

DELETE FROM user_roles
WHERE role_id IN (SELECT id FROM roles WHERE code = 'SECURITY');

DELETE FROM roles WHERE code = 'SECURITY';
