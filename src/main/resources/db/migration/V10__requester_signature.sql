-- ---------------------------------------------------------------
-- V10 - the creator signs too
--
-- The pass is printed and carried the moment it is raised, so the
-- person who raised it has to be signed for on that copy. Their
-- signature is snapshotted at CREATION, exactly the way the two
-- approvers' are snapshotted at approval, and for the same reason:
-- replacing a signature later must not restyle a document that was
-- already printed and filed.
--
-- Nullable, because a requester with no signature on file must
-- still be able to raise a pass - it prints their name over a
-- blank line rather than refusing.
-- ---------------------------------------------------------------
ALTER TABLE gate_pass_requests
    ADD COLUMN IF NOT EXISTS requester_signature_id UUID REFERENCES digital_signatures (id);
