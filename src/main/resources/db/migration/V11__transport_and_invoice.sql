-- =====================================================================
-- Transport and invoice details on a gate pass
-- =====================================================================
--
-- Three fields the paper form has always carried and the system
-- never did: who is hauling the consignment, and the invoice it
-- travels against.
--
-- All three are nullable. Every pass already in the table predates
-- them, and a NOT NULL column would mean inventing values for
-- historical records - which is the one thing an audit table must
-- never do. The app shows "Not recorded" for those, truthfully.

ALTER TABLE gate_pass_requests
    ADD COLUMN transport_name VARCHAR(255),
    ADD COLUMN invoice_number VARCHAR(100),
    ADD COLUMN invoice_date   DATE;

COMMENT ON COLUMN gate_pass_requests.transport_name IS
    'Transporter / carrier carrying the consignment.';

COMMENT ON COLUMN gate_pass_requests.invoice_number IS
    'Invoice the consignment travels against.';

COMMENT ON COLUMN gate_pass_requests.invoice_date IS
    'Date on that invoice. Not the dispatch date - an invoice is '
    'often raised days before the truck leaves.';

-- Finding a consignment by its invoice is the second thing anyone
-- asks at a gate, after the pass number itself.
CREATE INDEX idx_gate_pass_requests_invoice_number
    ON gate_pass_requests (invoice_number)
    WHERE invoice_number IS NOT NULL;
