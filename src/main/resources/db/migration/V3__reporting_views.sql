CREATE OR REPLACE VIEW v_pending_approvals AS
SELECT
    g.id,
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
JOIN vendors v ON v.id = g.vendor_id
JOIN drivers d ON d.id = g.driver_id
JOIN vehicles veh ON veh.id = g.vehicle_id
JOIN users u ON u.id = g.requested_by_user_id
WHERE g.deleted = FALSE
  AND g.status = 'PENDING';

CREATE OR REPLACE VIEW v_trucks_exited_today AS
SELECT
    g.id,
    g.gate_pass_number,
    veh.registration_number AS truck_number,
    v.name AS vendor_name,
    d.name AS driver_name,
    g.exit_time,
    w.name AS warehouse_name
FROM gate_pass_requests g
JOIN vehicles veh ON veh.id = g.vehicle_id
JOIN vendors v ON v.id = g.vendor_id
JOIN drivers d ON d.id = g.driver_id
JOIN warehouses w ON w.id = g.warehouse_id
WHERE g.deleted = FALSE
  AND g.exit_time::date = CURRENT_DATE;
