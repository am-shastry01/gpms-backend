-- =============================================================
-- Driver-facing gate pass link + SMS delivery
-- =============================================================
--
-- The driver is not an application user, so the pass has to reach
-- them over SMS as a link they can open with no login. That link is
-- addressed by a random token rather than the request's UUID, so it
-- can be expired, revoked and consumed once without touching the
-- primary key.

ALTER TABLE gate_pass_requests
    ADD COLUMN IF NOT EXISTS access_token VARCHAR(64);

ALTER TABLE gate_pass_requests
    ADD COLUMN IF NOT EXISTS token_issued_at TIMESTAMPTZ;

ALTER TABLE gate_pass_requests
    ADD COLUMN IF NOT EXISTS token_expires_at TIMESTAMPTZ;

-- Set the first time a guard successfully scans the pass, so a
-- screenshot of the QR cannot be reused to get a second truck in.
ALTER TABLE gate_pass_requests
    ADD COLUMN IF NOT EXISTS token_consumed_at TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS idx_gate_pass_access_token
    ON gate_pass_requests (access_token)
    WHERE access_token IS NOT NULL;


-- =============================================================
-- SMS sent to drivers
-- =============================================================
--
-- notifications.recipient_user_id is NOT NULL and a driver has no
-- user account, so driver messages get their own table. It doubles
-- as the delivery audit trail when a driver says they never got the
-- pass.

CREATE TABLE IF NOT EXISTS driver_notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gate_pass_request_id UUID NOT NULL REFERENCES gate_pass_requests(id),
    driver_id UUID REFERENCES drivers(id),
    mobile_number VARCHAR(20) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    link VARCHAR(500),
    -- SENT / FAILED / SKIPPED
    status VARCHAR(20) NOT NULL,
    provider VARCHAR(50),
    provider_message_id VARCHAR(120),
    error_detail VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_driver_notifications_request
    ON driver_notifications (gate_pass_request_id);

CREATE INDEX IF NOT EXISTS idx_driver_notifications_mobile
    ON driver_notifications (mobile_number);
