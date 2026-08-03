BEGIN;

INSERT INTO drivers (name, mobile_number, license_number, active, created_by, updated_by)
VALUES
    ('Rohit Sharma', '9876500011', 'DL-RS-1001', TRUE, 'system', 'system'),
    ('Suresh Patel', '9876500012', 'DL-SP-1002', TRUE, 'system', 'system'),
    ('Imran Khan', '9876500013', 'DL-IK-1003', TRUE, 'system', 'system'),
    ('Manoj Yadav', '9876500014', 'DL-MY-1004', TRUE, 'system', 'system'),
    ('Deepak Verma', '9876500015', 'DL-DV-1005', TRUE, 'system', 'system')
ON CONFLICT (mobile_number) DO NOTHING;

INSERT INTO vehicles (vendor_id, registration_number, vehicle_type, capacity, active, created_by, updated_by)
SELECT v.id, x.registration_number, x.vehicle_type, x.capacity, TRUE, 'system', 'system'
FROM (
    VALUES
        ('DELHIVERY', 'MP09GH2871', 'Truck', '9 Tons'),
        ('DTDC', 'MH12AB4587', 'Van', '3 Tons'),
        ('BLUEDART', 'UP32JK9087', 'Truck', '7 Tons'),
        ('OTHERS', 'RJ14TT2211', 'Container', '12 Tons'),
        ('DELHIVERY', 'GJ05MN7788', 'Pickup', '2 Tons')
) AS x(vendor_code, registration_number, vehicle_type, capacity)
JOIN vendors v ON v.code = x.vendor_code
ON CONFLICT (registration_number) DO NOTHING;

DO $$
DECLARE
    wh_id UUID;
    employee_id UUID;
    manager_id UUID;
    security_id UUID;
    admin_id UUID;

    delhivery_id UUID;
    dtdc_id UUID;
    bluedart_id UUID;
    others_id UUID;

    driver_rohit_id UUID;
    driver_suresh_id UUID;
    driver_imran_id UUID;
    driver_manoj_id UUID;
    driver_deepak_id UUID;

    vehicle_mp_id UUID;
    vehicle_mh_id UUID;
    vehicle_up_id UUID;
    vehicle_rj_id UUID;
    vehicle_gj_id UUID;

    req1_id UUID;
    req2_id UUID;
    req3_id UUID;
    req4_id UUID;
    req5_id UUID;
    req6_id UUID;
BEGIN
    SELECT id INTO wh_id FROM warehouses WHERE code = 'WH-01';
    SELECT id INTO employee_id FROM users WHERE username = 'employee';
    SELECT id INTO manager_id FROM users WHERE username = 'manager';
    SELECT id INTO security_id FROM users WHERE username = 'security';
    SELECT id INTO admin_id FROM users WHERE username = 'admin';

    SELECT id INTO delhivery_id FROM vendors WHERE code = 'DELHIVERY';
    SELECT id INTO dtdc_id FROM vendors WHERE code = 'DTDC';
    SELECT id INTO bluedart_id FROM vendors WHERE code = 'BLUEDART';
    SELECT id INTO others_id FROM vendors WHERE code = 'OTHERS';

    SELECT id INTO driver_rohit_id FROM drivers WHERE mobile_number = '9876500011';
    SELECT id INTO driver_suresh_id FROM drivers WHERE mobile_number = '9876500012';
    SELECT id INTO driver_imran_id FROM drivers WHERE mobile_number = '9876500013';
    SELECT id INTO driver_manoj_id FROM drivers WHERE mobile_number = '9876500014';
    SELECT id INTO driver_deepak_id FROM drivers WHERE mobile_number = '9876500015';

    SELECT id INTO vehicle_mp_id FROM vehicles WHERE registration_number = 'MP09GH2871';
    SELECT id INTO vehicle_mh_id FROM vehicles WHERE registration_number = 'MH12AB4587';
    SELECT id INTO vehicle_up_id FROM vehicles WHERE registration_number = 'UP32JK9087';
    SELECT id INTO vehicle_rj_id FROM vehicles WHERE registration_number = 'RJ14TT2211';
    SELECT id INTO vehicle_gj_id FROM vehicles WHERE registration_number = 'GJ05MN7788';

    INSERT INTO gate_pass_requests (
        request_number,
        gate_pass_number,
        warehouse_id,
        requested_by_user_id,
        approved_by_user_id,
        exited_by_user_id,
        vendor_id,
        driver_id,
        vehicle_id,
        status,
        dispatch_date,
        package_count,
        package_description,
        destination,
        remarks,
        manager_comments,
        approval_time,
        exit_time,
        qr_content,
        created_by,
        updated_by
    )
    VALUES (
        'REQ-2026-000001',
        NULL,
        wh_id,
        employee_id,
        NULL,
        NULL,
        delhivery_id,
        driver_rohit_id,
        vehicle_mp_id,
        'PENDING',
        DATE '2026-08-03',
        80,
        'E-commerce parcels',
        'Indore DC',
        '[SAMPLE] Pending approval for morning dispatch',
        NULL,
        NULL,
        NULL,
        NULL,
        'employee',
        'employee'
    )
    ON CONFLICT (request_number) DO NOTHING
    RETURNING id INTO req1_id;
    IF req1_id IS NULL THEN
        SELECT id INTO req1_id FROM gate_pass_requests WHERE request_number = 'REQ-2026-000001';
    END IF;

    INSERT INTO gate_pass_requests (
        request_number,
        gate_pass_number,
        warehouse_id,
        requested_by_user_id,
        approved_by_user_id,
        exited_by_user_id,
        vendor_id,
        driver_id,
        vehicle_id,
        status,
        dispatch_date,
        package_count,
        package_description,
        destination,
        remarks,
        manager_comments,
        approval_time,
        exit_time,
        qr_content,
        created_by,
        updated_by
    )
    VALUES (
        'REQ-2026-000002',
        'GP-2026-000001',
        wh_id,
        employee_id,
        manager_id,
        NULL,
        dtdc_id,
        driver_suresh_id,
        vehicle_mh_id,
        'GATE_GENERATED',
        DATE '2026-08-03',
        45,
        'Documents and cartons',
        'Bhopal Hub',
        '[SAMPLE] Approved and ready at gate',
        'Approved for dispatch.',
        TIMESTAMPTZ '2026-08-03 09:20:00+05:30',
        NULL,
        '{"gatePassId":"REQ-2026-000002","gatePassNumber":"GP-2026-000001","truckNumber":"MH12AB4587","vendor":"DTDC","status":"GATE_GENERATED"}',
        'manager',
        'manager'
    )
    ON CONFLICT (request_number) DO NOTHING
    RETURNING id INTO req2_id;
    IF req2_id IS NULL THEN
        SELECT id INTO req2_id FROM gate_pass_requests WHERE request_number = 'REQ-2026-000002';
    END IF;

    INSERT INTO gate_pass_requests (
        request_number,
        gate_pass_number,
        warehouse_id,
        requested_by_user_id,
        approved_by_user_id,
        exited_by_user_id,
        vendor_id,
        driver_id,
        vehicle_id,
        status,
        dispatch_date,
        package_count,
        package_description,
        destination,
        remarks,
        manager_comments,
        approval_time,
        exit_time,
        qr_content,
        created_by,
        updated_by
    )
    VALUES (
        'REQ-2026-000003',
        NULL,
        wh_id,
        employee_id,
        manager_id,
        NULL,
        bluedart_id,
        driver_imran_id,
        vehicle_up_id,
        'REJECTED',
        DATE '2026-08-03',
        30,
        'Electronic accessories',
        'Gwalior Cross Dock',
        '[SAMPLE] Rejected due to documentation mismatch',
        'Rejected. Dispatch note missing package verification.',
        TIMESTAMPTZ '2026-08-03 10:10:00+05:30',
        NULL,
        NULL,
        'manager',
        'manager'
    )
    ON CONFLICT (request_number) DO NOTHING
    RETURNING id INTO req3_id;
    IF req3_id IS NULL THEN
        SELECT id INTO req3_id FROM gate_pass_requests WHERE request_number = 'REQ-2026-000003';
    END IF;

    INSERT INTO gate_pass_requests (
        request_number,
        gate_pass_number,
        warehouse_id,
        requested_by_user_id,
        approved_by_user_id,
        exited_by_user_id,
        vendor_id,
        driver_id,
        vehicle_id,
        status,
        dispatch_date,
        package_count,
        package_description,
        destination,
        remarks,
        manager_comments,
        approval_time,
        exit_time,
        qr_content,
        created_by,
        updated_by
    )
    VALUES (
        'REQ-2026-000004',
        'GP-2026-000002',
        wh_id,
        employee_id,
        manager_id,
        security_id,
        delhivery_id,
        driver_rohit_id,
        vehicle_mp_id,
        'EXITED',
        DATE '2026-08-02',
        120,
        'Bulk outbound parcels',
        'Nagpur Regional Hub',
        '[SAMPLE] Exited successfully from Gate 2',
        'Approved after dock verification.',
        TIMESTAMPTZ '2026-08-02 11:00:00+05:30',
        TIMESTAMPTZ '2026-08-02 12:30:00+05:30',
        '{"gatePassId":"REQ-2026-000004","gatePassNumber":"GP-2026-000002","truckNumber":"MP09GH2871","vendor":"Delhivery","status":"EXITED"}',
        'security',
        'security'
    )
    ON CONFLICT (request_number) DO NOTHING
    RETURNING id INTO req4_id;
    IF req4_id IS NULL THEN
        SELECT id INTO req4_id FROM gate_pass_requests WHERE request_number = 'REQ-2026-000004';
    END IF;

    INSERT INTO gate_pass_requests (
        request_number,
        gate_pass_number,
        warehouse_id,
        requested_by_user_id,
        approved_by_user_id,
        exited_by_user_id,
        vendor_id,
        driver_id,
        vehicle_id,
        status,
        dispatch_date,
        package_count,
        package_description,
        destination,
        remarks,
        manager_comments,
        approval_time,
        exit_time,
        qr_content,
        created_by,
        updated_by
    )
    VALUES (
        'REQ-2026-000005',
        'GP-2026-000003',
        wh_id,
        employee_id,
        manager_id,
        NULL,
        others_id,
        driver_manoj_id,
        vehicle_rj_id,
        'GATE_GENERATED',
        DATE '2026-08-03',
        20,
        'Returns and reverse logistics',
        'Jaipur Aggregation Point',
        '[SAMPLE] Approved and waiting for security scan',
        'Approved with reverse logistics checklist.',
        TIMESTAMPTZ '2026-08-03 11:45:00+05:30',
        NULL,
        '{"gatePassId":"REQ-2026-000005","gatePassNumber":"GP-2026-000003","truckNumber":"RJ14TT2211","vendor":"Others","status":"GATE_GENERATED"}',
        'manager',
        'manager'
    )
    ON CONFLICT (request_number) DO NOTHING
    RETURNING id INTO req5_id;
    IF req5_id IS NULL THEN
        SELECT id INTO req5_id FROM gate_pass_requests WHERE request_number = 'REQ-2026-000005';
    END IF;

    INSERT INTO gate_pass_requests (
        request_number,
        gate_pass_number,
        warehouse_id,
        requested_by_user_id,
        approved_by_user_id,
        exited_by_user_id,
        vendor_id,
        driver_id,
        vehicle_id,
        status,
        dispatch_date,
        package_count,
        package_description,
        destination,
        remarks,
        manager_comments,
        approval_time,
        exit_time,
        qr_content,
        created_by,
        updated_by
    )
    VALUES (
        'REQ-2026-000006',
        'GP-2026-000004',
        wh_id,
        employee_id,
        manager_id,
        security_id,
        delhivery_id,
        driver_deepak_id,
        vehicle_gj_id,
        'EXITED',
        DATE '2026-08-03',
        16,
        'Priority same-day shipments',
        'Ujjain City Route',
        '[SAMPLE] Cleared and exited from Gate 1',
        'Approved for priority dispatch.',
        TIMESTAMPTZ '2026-08-03 12:05:00+05:30',
        TIMESTAMPTZ '2026-08-03 12:50:00+05:30',
        '{"gatePassId":"REQ-2026-000006","gatePassNumber":"GP-2026-000004","truckNumber":"GJ05MN7788","vendor":"Delhivery","status":"EXITED"}',
        'security',
        'security'
    )
    ON CONFLICT (request_number) DO NOTHING
    RETURNING id INTO req6_id;
    IF req6_id IS NULL THEN
        SELECT id INTO req6_id FROM gate_pass_requests WHERE request_number = 'REQ-2026-000006';
    END IF;

    INSERT INTO gate_pass_items (gate_pass_request_id, line_number, item_description, quantity, unit_of_measure, created_by, updated_by)
    VALUES
        (req1_id, 1, 'Mobile accessories cartons', 50, 'boxes', 'employee', 'employee'),
        (req1_id, 2, 'Barcode label sleeves', 30, 'bundles', 'employee', 'employee'),
        (req2_id, 1, 'Confidential document bags', 20, 'bags', 'employee', 'employee'),
        (req2_id, 2, 'Dispatch cartons', 25, 'boxes', 'employee', 'employee'),
        (req3_id, 1, 'Electronic accessories master cartons', 30, 'boxes', 'employee', 'employee'),
        (req4_id, 1, 'Outbound parcel sacks', 120, 'sacks', 'employee', 'employee'),
        (req5_id, 1, 'Reverse pickup totes', 12, 'totes', 'employee', 'employee'),
        (req5_id, 2, 'Returns documentation packs', 8, 'packs', 'employee', 'employee'),
        (req6_id, 1, 'Priority courier bags', 16, 'bags', 'employee', 'employee')
    ON CONFLICT ON CONSTRAINT uq_gate_pass_item_line DO NOTHING;

    INSERT INTO approvals (gate_pass_request_id, manager_user_id, action, comments, action_time, created_by, updated_by)
    SELECT req2_id, manager_id, 'APPROVED', 'Approved for dispatch.', TIMESTAMPTZ '2026-08-03 09:20:00+05:30', 'manager', 'manager'
    WHERE NOT EXISTS (
        SELECT 1 FROM approvals WHERE gate_pass_request_id = req2_id AND action = 'APPROVED'
    );

    INSERT INTO approvals (gate_pass_request_id, manager_user_id, action, comments, action_time, created_by, updated_by)
    SELECT req3_id, manager_id, 'REJECTED', 'Rejected. Dispatch note missing package verification.', TIMESTAMPTZ '2026-08-03 10:10:00+05:30', 'manager', 'manager'
    WHERE NOT EXISTS (
        SELECT 1 FROM approvals WHERE gate_pass_request_id = req3_id AND action = 'REJECTED'
    );

    INSERT INTO approvals (gate_pass_request_id, manager_user_id, action, comments, action_time, created_by, updated_by)
    SELECT req4_id, manager_id, 'APPROVED', 'Approved after dock verification.', TIMESTAMPTZ '2026-08-02 11:00:00+05:30', 'manager', 'manager'
    WHERE NOT EXISTS (
        SELECT 1 FROM approvals WHERE gate_pass_request_id = req4_id AND action = 'APPROVED'
    );

    INSERT INTO approvals (gate_pass_request_id, manager_user_id, action, comments, action_time, created_by, updated_by)
    SELECT req5_id, manager_id, 'APPROVED', 'Approved with reverse logistics checklist.', TIMESTAMPTZ '2026-08-03 11:45:00+05:30', 'manager', 'manager'
    WHERE NOT EXISTS (
        SELECT 1 FROM approvals WHERE gate_pass_request_id = req5_id AND action = 'APPROVED'
    );

    INSERT INTO approvals (gate_pass_request_id, manager_user_id, action, comments, action_time, created_by, updated_by)
    SELECT req6_id, manager_id, 'APPROVED', 'Approved for priority dispatch.', TIMESTAMPTZ '2026-08-03 12:05:00+05:30', 'manager', 'manager'
    WHERE NOT EXISTS (
        SELECT 1 FROM approvals WHERE gate_pass_request_id = req6_id AND action = 'APPROVED'
    );

    INSERT INTO notifications (recipient_user_id, gate_pass_request_id, channel, status, title, message, sent_at, read_at, created_by, updated_by)
    SELECT employee_id, req2_id, 'IN_APP', 'SENT', 'Gate pass approved', 'Gate Pass GP-2026-000001 approved for truck MH12AB4587.', TIMESTAMPTZ '2026-08-03 09:21:00+05:30', NULL, 'system', 'system'
    WHERE NOT EXISTS (
        SELECT 1 FROM notifications WHERE recipient_user_id = employee_id AND gate_pass_request_id = req2_id AND title = 'Gate pass approved'
    );

    INSERT INTO notifications (recipient_user_id, gate_pass_request_id, channel, status, title, message, sent_at, read_at, created_by, updated_by)
    SELECT security_id, req2_id, 'IN_APP', 'SENT', 'Gate pass approved', 'Truck MH12AB4587 is ready for gate verification.', TIMESTAMPTZ '2026-08-03 09:21:00+05:30', NULL, 'system', 'system'
    WHERE NOT EXISTS (
        SELECT 1 FROM notifications WHERE recipient_user_id = security_id AND gate_pass_request_id = req2_id AND title = 'Gate pass approved'
    );

    INSERT INTO notifications (recipient_user_id, gate_pass_request_id, channel, status, title, message, sent_at, read_at, created_by, updated_by)
    SELECT employee_id, req3_id, 'IN_APP', 'READ', 'Gate pass rejected', 'Request REQ-2026-000003 was rejected due to missing package verification.', TIMESTAMPTZ '2026-08-03 10:11:00+05:30', TIMESTAMPTZ '2026-08-03 10:20:00+05:30', 'system', 'system'
    WHERE NOT EXISTS (
        SELECT 1 FROM notifications WHERE recipient_user_id = employee_id AND gate_pass_request_id = req3_id AND title = 'Gate pass rejected'
    );

    INSERT INTO notifications (recipient_user_id, gate_pass_request_id, channel, status, title, message, sent_at, read_at, created_by, updated_by)
    SELECT manager_id, req6_id, 'IN_APP', 'SENT', 'Truck exited', 'Gate Pass GP-2026-000004 exited at 12:50 PM on August 3, 2026.', TIMESTAMPTZ '2026-08-03 12:51:00+05:30', NULL, 'system', 'system'
    WHERE NOT EXISTS (
        SELECT 1 FROM notifications WHERE recipient_user_id = manager_id AND gate_pass_request_id = req6_id AND title = 'Truck exited'
    );

    INSERT INTO notifications (recipient_user_id, gate_pass_request_id, channel, status, title, message, sent_at, read_at, created_by, updated_by)
    SELECT admin_id, req6_id, 'IN_APP', 'SENT', 'Truck exited', 'Gate Pass GP-2026-000004 exited successfully from the warehouse.', TIMESTAMPTZ '2026-08-03 12:51:00+05:30', NULL, 'system', 'system'
    WHERE NOT EXISTS (
        SELECT 1 FROM notifications WHERE recipient_user_id = admin_id AND gate_pass_request_id = req6_id AND title = 'Truck exited'
    );

    INSERT INTO audit_logs (actor_user_id, entity_type, entity_id, action, before_state, after_state, metadata_json, created_by, updated_by)
    SELECT employee_id, 'GatePassRequest', req1_id::text, 'CREATED', NULL, 'PENDING', '{"requestNumber":"REQ-2026-000001"}', 'employee', 'employee'
    WHERE NOT EXISTS (
        SELECT 1 FROM audit_logs WHERE entity_type = 'GatePassRequest' AND entity_id = req1_id::text AND action = 'CREATED'
    );

    INSERT INTO audit_logs (actor_user_id, entity_type, entity_id, action, before_state, after_state, metadata_json, created_by, updated_by)
    SELECT manager_id, 'GatePassRequest', req2_id::text, 'APPROVED', 'PENDING', 'GATE_GENERATED', '{"gatePassNumber":"GP-2026-000001"}', 'manager', 'manager'
    WHERE NOT EXISTS (
        SELECT 1 FROM audit_logs WHERE entity_type = 'GatePassRequest' AND entity_id = req2_id::text AND action = 'APPROVED'
    );

    INSERT INTO audit_logs (actor_user_id, entity_type, entity_id, action, before_state, after_state, metadata_json, created_by, updated_by)
    SELECT manager_id, 'GatePassRequest', req3_id::text, 'REJECTED', 'PENDING', 'REJECTED', '{"reason":"Dispatch note missing package verification"}', 'manager', 'manager'
    WHERE NOT EXISTS (
        SELECT 1 FROM audit_logs WHERE entity_type = 'GatePassRequest' AND entity_id = req3_id::text AND action = 'REJECTED'
    );

    INSERT INTO audit_logs (actor_user_id, entity_type, entity_id, action, before_state, after_state, metadata_json, created_by, updated_by)
    SELECT security_id, 'GatePassRequest', req6_id::text, 'EXITED', 'GATE_GENERATED', 'EXITED', '{"gatePassNumber":"GP-2026-000004"}', 'security', 'security'
    WHERE NOT EXISTS (
        SELECT 1 FROM audit_logs WHERE entity_type = 'GatePassRequest' AND entity_id = req6_id::text AND action = 'EXITED'
    );
END $$;

SELECT setval(
    'request_number_seq',
    GREATEST(
        COALESCE((SELECT MAX((RIGHT(request_number, 6))::BIGINT) FROM gate_pass_requests), 1),
        1
    ),
    TRUE
);

SELECT setval(
    'gate_pass_number_seq',
    GREATEST(
        COALESCE((SELECT MAX((RIGHT(gate_pass_number, 6))::BIGINT) FROM gate_pass_requests WHERE gate_pass_number IS NOT NULL), 1),
        1
    ),
    TRUE
);

COMMIT;
