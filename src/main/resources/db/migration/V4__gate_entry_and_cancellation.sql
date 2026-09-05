-- Gate ENTRY tracking.
--
-- The schema only ever recorded a truck leaving (exit_time /
-- exited_by_user_id). The guard app also needs to record a truck
-- arriving at the gate, so the mirror-image columns are added here.
ALTER TABLE gate_pass_requests
    ADD COLUMN IF NOT EXISTS entry_time TIMESTAMPTZ;

ALTER TABLE gate_pass_requests
    ADD COLUMN IF NOT EXISTS entered_by_user_id UUID REFERENCES users(id);

CREATE INDEX IF NOT EXISTS idx_gate_pass_entry_time
    ON gate_pass_requests (entry_time);

-- status is a plain VARCHAR(30) with no CHECK constraint, so the two
-- new values (ENTERED, CANCELLED) need no schema change. The index
-- below keeps the guard dashboard's status filtering cheap.
CREATE INDEX IF NOT EXISTS idx_gate_pass_status_dispatch
    ON gate_pass_requests (status, dispatch_date);
