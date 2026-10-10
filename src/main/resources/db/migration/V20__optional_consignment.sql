-- =====================================================================
-- A gate pass no longer has to name a vendor.
--
-- One truck leaving the yard is one pass, and a pass is raised for the
-- MOVEMENT - the vehicle, the driver, the destination. The consignments
-- on board are lines under it, and sometimes there are none: an empty
-- vehicle leaving, a return trip, a pass raised at the gate before the
-- paperwork has caught up. Until now vendor_id was NOT NULL, so a pass
-- could not exist without naming somebody to be carrying something,
-- and the app had to invent a consignment to satisfy the column.
--
-- Nothing is dropped and no data moves: every existing pass keeps the
-- vendor it has. Only the requirement goes.
-- =====================================================================

ALTER TABLE gate_pass_requests
    ALTER COLUMN vendor_id DROP NOT NULL;

-- ---------------------------------------------------------------------
-- A pass may now count zero packages.
--
-- Both of these were CHECK (> 0), written when every pass had to carry
-- something. A pass with no consignment counts nothing, and a line
-- recorded without a quantity counts nothing either - so the floor
-- moves from one to zero. Negative stays impossible, which is the part
-- of the constraint that was ever protecting anything.
-- ---------------------------------------------------------------------

ALTER TABLE gate_pass_requests
    DROP CONSTRAINT IF EXISTS gate_pass_requests_package_count_check;

ALTER TABLE gate_pass_requests
    ADD CONSTRAINT gate_pass_requests_package_count_check
    CHECK (package_count >= 0);

ALTER TABLE gate_pass_items
    DROP CONSTRAINT IF EXISTS gate_pass_items_quantity_check;

ALTER TABLE gate_pass_items
    ADD CONSTRAINT gate_pass_items_quantity_check
    CHECK (quantity >= 0);

-- ---------------------------------------------------------------------
-- v_pending_approvals has to stop hiding passes.
--
-- It joined vendors with an INNER JOIN, written when vendor_id could
-- not be null. Now that it can, a pass with no consignment would
-- simply not appear in this view - not an error, which is worse: a
-- pending pass missing from a list of pending passes.
--
-- Nothing else about the view changes. The vendor_name column is now
-- NULL for such a pass, which is the truth about it.
--
-- No application code reads this view today; it is fixed because a
-- view that quietly drops rows is a trap for whoever reads it next.
-- ---------------------------------------------------------------------

CREATE OR REPLACE VIEW v_pending_approvals AS
SELECT g.id,
       g.request_number,
       g.dispatch_date,
       g.package_count,
       g.destination,
       w.code AS warehouse_code,
       w.name AS warehouse_name,
       v.name AS vendor_name,
       d.name AS driver_name,
       d.mobile_number AS driver_mobile,
       veh.registration_number AS truck_number,
       u.full_name AS requested_by
FROM gate_pass_requests g
         JOIN warehouses w ON w.id = g.warehouse_id
         LEFT JOIN vendors v ON v.id = g.vendor_id
         JOIN drivers d ON d.id = g.driver_id
         JOIN vehicles veh ON veh.id = g.vehicle_id
         JOIN users u ON u.id = g.requested_by_user_id
WHERE g.deleted = FALSE
  AND g.status = 'PENDING';
