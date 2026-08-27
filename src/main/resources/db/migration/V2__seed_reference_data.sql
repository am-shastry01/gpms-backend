INSERT INTO roles (code, description)
VALUES
    ('ADMIN', 'Administrator role'),
    ('MANAGER', 'Warehouse manager role'),
    ('EMPLOYEE', 'Warehouse employee role'),
    ('SECURITY', 'Security guard role')
ON CONFLICT (code) DO NOTHING;

INSERT INTO vendors (code, name, contact_person, phone_number, email)
VALUES
    ('DELHIVERY', 'Delhivery', 'Dispatch Desk', '+91-9000000001', 'ops@delhivery.local'),
    ('DTDC', 'DTDC', 'Dispatch Desk', '+91-9000000002', 'ops@dtdc.local'),
    ('BLUEDART', 'Blue Dart', 'Dispatch Desk', '+91-9000000003', 'ops@bluedart.local'),
    ('OTHERS', 'Others', 'Dispatch Desk', '+91-9000000004', 'ops@others.local')
ON CONFLICT (code) DO NOTHING;
