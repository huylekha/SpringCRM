-- Seed RBAC Default Data
-- Compatible with PostgreSQL 16+

-- Insert default permissions
INSERT INTO auth_permission (id, name, resource, action, description, created_by) VALUES
    (gen_random_uuid(), 'USER_READ', 'USER', 'READ', 'Read user information', 'SYSTEM'),
    (gen_random_uuid(), 'USER_WRITE', 'USER', 'WRITE', 'Create and update users', 'SYSTEM'),
    (gen_random_uuid(), 'USER_DELETE', 'USER', 'DELETE', 'Delete users', 'SYSTEM'),
    (gen_random_uuid(), 'ROLE_READ', 'ROLE', 'READ', 'Read role information', 'SYSTEM'),
    (gen_random_uuid(), 'ROLE_WRITE', 'ROLE', 'WRITE', 'Create and update roles', 'SYSTEM'),
    (gen_random_uuid(), 'ROLE_DELETE', 'ROLE', 'DELETE', 'Delete roles', 'SYSTEM'),
    (gen_random_uuid(), 'PERMISSION_READ', 'PERMISSION', 'READ', 'Read permission information', 'SYSTEM'),
    (gen_random_uuid(), 'PERMISSION_WRITE', 'PERMISSION', 'WRITE', 'Create and update permissions', 'SYSTEM'),
    (gen_random_uuid(), 'CRM_READ', 'CRM', 'READ', 'Read CRM data', 'SYSTEM'),
    (gen_random_uuid(), 'CRM_WRITE', 'CRM', 'WRITE', 'Create and update CRM data', 'SYSTEM'),
    (gen_random_uuid(), 'CRM_DELETE', 'CRM', 'DELETE', 'Delete CRM data', 'SYSTEM'),
    (gen_random_uuid(), 'CUSTOMER_READ', 'CUSTOMER', 'READ', 'Read customer information', 'SYSTEM'),
    (gen_random_uuid(), 'CUSTOMER_WRITE', 'CUSTOMER', 'WRITE', 'Create and update customers', 'SYSTEM'),
    (gen_random_uuid(), 'CUSTOMER_DELETE', 'CUSTOMER', 'DELETE', 'Delete customers', 'SYSTEM'),
    (gen_random_uuid(), 'LEAD_READ', 'LEAD', 'READ', 'Read lead information', 'SYSTEM'),
    (gen_random_uuid(), 'LEAD_WRITE', 'LEAD', 'WRITE', 'Create and update leads', 'SYSTEM'),
    (gen_random_uuid(), 'LEAD_DELETE', 'LEAD', 'DELETE', 'Delete leads', 'SYSTEM'),
    (gen_random_uuid(), 'OPPORTUNITY_READ', 'OPPORTUNITY', 'READ', 'Read opportunity information', 'SYSTEM'),
    (gen_random_uuid(), 'OPPORTUNITY_WRITE', 'OPPORTUNITY', 'WRITE', 'Create and update opportunities', 'SYSTEM'),
    (gen_random_uuid(), 'OPPORTUNITY_DELETE', 'OPPORTUNITY', 'DELETE', 'Delete opportunities', 'SYSTEM');

-- Insert default claims
INSERT INTO auth_claim (id, name, description, created_by) VALUES
    (gen_random_uuid(), 'ADMIN', 'Full system administrator access', 'SYSTEM'),
    (gen_random_uuid(), 'MANAGER', 'Management level access', 'SYSTEM'),
    (gen_random_uuid(), 'SALES_REP', 'Sales representative access', 'SYSTEM'),
    (gen_random_uuid(), 'VIEWER', 'Read-only access', 'SYSTEM');

-- Insert default roles
INSERT INTO auth_role (id, name, display_name, description, is_system, created_by) VALUES
    (gen_random_uuid(), 'SUPER_ADMIN', 'Super Administrator', 'Full system access with all permissions', true, 'SYSTEM'),
    (gen_random_uuid(), 'ADMIN', 'Administrator', 'Administrative access to manage users and system', true, 'SYSTEM'),
    (gen_random_uuid(), 'SALES_MANAGER', 'Sales Manager', 'Manage sales team and access all CRM data', false, 'SYSTEM'),
    (gen_random_uuid(), 'SALES_REP', 'Sales Representative', 'Access to assigned customers and leads', false, 'SYSTEM'),
    (gen_random_uuid(), 'VIEWER', 'Viewer', 'Read-only access to CRM data', false, 'SYSTEM');

-- Assign permissions to SUPER_ADMIN role (all permissions)
INSERT INTO auth_role_permission (role_id, permission_id)
SELECT 
    (SELECT id FROM auth_role WHERE name = 'SUPER_ADMIN'),
    p.id
FROM auth_permission p;

-- Assign permissions to ADMIN role (user and role management)
INSERT INTO auth_role_permission (role_id, permission_id)
SELECT 
    (SELECT id FROM auth_role WHERE name = 'ADMIN'),
    p.id
FROM auth_permission p
WHERE p.resource IN ('USER', 'ROLE', 'PERMISSION');

-- Assign permissions to SALES_MANAGER role (all CRM permissions)
INSERT INTO auth_role_permission (role_id, permission_id)
SELECT 
    (SELECT id FROM auth_role WHERE name = 'SALES_MANAGER'),
    p.id
FROM auth_permission p
WHERE p.resource IN ('CRM', 'CUSTOMER', 'LEAD', 'OPPORTUNITY');

-- Assign permissions to SALES_REP role (read/write CRM, no delete)
INSERT INTO auth_role_permission (role_id, permission_id)
SELECT 
    (SELECT id FROM auth_role WHERE name = 'SALES_REP'),
    p.id
FROM auth_permission p
WHERE p.resource IN ('CUSTOMER', 'LEAD', 'OPPORTUNITY') 
  AND p.action IN ('READ', 'WRITE');

-- Assign permissions to VIEWER role (read-only)
INSERT INTO auth_role_permission (role_id, permission_id)
SELECT 
    (SELECT id FROM auth_role WHERE name = 'VIEWER'),
    p.id
FROM auth_permission p
WHERE p.action = 'READ';

-- Assign claims to roles
INSERT INTO auth_role_claim (role_id, claim_id)
VALUES
    ((SELECT id FROM auth_role WHERE name = 'SUPER_ADMIN'), (SELECT id FROM auth_claim WHERE name = 'ADMIN')),
    ((SELECT id FROM auth_role WHERE name = 'ADMIN'), (SELECT id FROM auth_claim WHERE name = 'ADMIN')),
    ((SELECT id FROM auth_role WHERE name = 'SALES_MANAGER'), (SELECT id FROM auth_claim WHERE name = 'MANAGER')),
    ((SELECT id FROM auth_role WHERE name = 'SALES_REP'), (SELECT id FROM auth_claim WHERE name = 'SALES_REP')),
    ((SELECT id FROM auth_role WHERE name = 'VIEWER'), (SELECT id FROM auth_claim WHERE name = 'VIEWER'));

-- Create default admin user (password: admin123)
-- Password hash for 'admin123' using BCrypt
INSERT INTO auth_user (id, username, email, password_hash, first_name, last_name, is_active, is_verified, created_by)
VALUES (
    gen_random_uuid(),
    'admin',
    'admin@company.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewfBzj6BnVfwzfHy', -- admin123
    'System',
    'Administrator',
    true,
    true,
    'SYSTEM'
);

-- Assign SUPER_ADMIN role to admin user
INSERT INTO auth_user_role (user_id, role_id)
VALUES (
    (SELECT id FROM auth_user WHERE username = 'admin'),
    (SELECT id FROM auth_role WHERE name = 'SUPER_ADMIN')
);