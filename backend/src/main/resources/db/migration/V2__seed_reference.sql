-- =============================================================================
-- V2 · Seed reference data
-- Idempotent-friendly: uses ON CONFLICT DO NOTHING so the migration can be
-- re-run against a DB that already has the rows without failing.
-- =============================================================================

INSERT INTO role (code, description) VALUES
    ('ADMIN',       'Full system administrator'),
    ('IT_MANAGER',  'IT operations manager: day-to-day asset, license and ticket work'),
    ('EMPLOYEE',    'Regular employee: self-service view of own assets and tickets')
ON CONFLICT (code) DO NOTHING;

INSERT INTO asset_category (code, name, description) VALUES
    ('LAPTOP',     'Laptop',      'Portable computer'),
    ('DESKTOP',    'Desktop',     'Non-portable workstation'),
    ('MONITOR',    'Monitor',     'External display'),
    ('PHONE',      'Phone',       'Company-issued smartphone'),
    ('TABLET',     'Tablet',      'Company-issued tablet'),
    ('DOCK',       'Dock',        'Docking station / hub'),
    ('PERIPHERAL', 'Peripheral',  'Keyboard, mouse, headset, webcam, etc.'),
    ('OTHER',      'Other',       'Anything not covered above')
ON CONFLICT (code) DO NOTHING;
