-- Seed RBAC Default Data
-- Uses gen_random_uuid() for UUID v4; application layer uses UUID v7 for normal entities.
-- Seed rows use a fixed system tenant and SYSTEM auditor.

-- Insert default permissions
INSERT INTO auth_permission (id, permission_code, resource_name, action_name, tenant_id, created_at, created_by_name, deleted)
VALUES
    (gen_random_uuid(), 'USER_READ',         'USER',        'READ',   'SYSTEM', NOW(), 'SYSTEM', FALSE),
    (gen_random_uuid(), 'USER_WRITE',        'USER',        'WRITE',  'SYSTEM', NOW(), 'SYSTEM', FALSE),
    (gen_random_uuid(), 'USER_DELETE',        'USER',        'DELETE', 'SYSTEM', NOW(), 'SYSTEM', FALSE),
    (gen_random_uuid(), 'ROLE_READ',         'ROLE',        'READ',   'SYSTEM', NOW(), 'SYSTEM', FALSE),
    (gen_random_uuid(), 'ROLE_WRITE',        'ROLE',        'WRITE',  'SYSTEM', NOW(), 'SYSTEM', FALSE),
    (gen_random_uuid(), 'ROLE_DELETE',        'ROLE',        'DELETE', 'SYSTEM', NOW(), 'SYSTEM', FALSE),
    (gen_random_uuid(), 'PERMISSION_READ',   'PERMISSION',  'READ',   'SYSTEM', NOW(), 'SYSTEM', FALSE),
    (gen_random_uuid(), 'PERMISSION_WRITE',  'PERMISSION',  'WRITE',  'SYSTEM', NOW(), 'SYSTEM', FALSE),
    (gen_random_uuid(), 'CRM_READ',          'CRM',         'READ',   'SYSTEM', NOW(), 'SYSTEM', FALSE),
    (gen_random_uuid(), 'CRM_WRITE',         'CRM',         'WRITE',  'SYSTEM', NOW(), 'SYSTEM', FALSE),
    (gen_random_uuid(), 'CRM_DELETE',         'CRM',         'DELETE', 'SYSTEM', NOW(), 'SYSTEM', FALSE),
    (gen_random_uuid(), 'CUSTOMER_READ',     'CUSTOMER',    'READ',   'SYSTEM', NOW(), 'SYSTEM', FALSE),
    (gen_random_uuid(), 'CUSTOMER_WRITE',    'CUSTOMER',    'WRITE',  'SYSTEM', NOW(), 'SYSTEM', FALSE),
    (gen_random_uuid(), 'CUSTOMER_DELETE',   'CUSTOMER',    'DELETE', 'SYSTEM', NOW(), 'SYSTEM', FALSE),
    (gen_random_uuid(), 'LEAD_READ',         'LEAD',        'READ',   'SYSTEM', NOW(), 'SYSTEM', FALSE),
    (gen_random_uuid(), 'LEAD_WRITE',        'LEAD',        'WRITE',  'SYSTEM', NOW(), 'SYSTEM', FALSE),
    (gen_random_uuid(), 'LEAD_DELETE',       'LEAD',        'DELETE', 'SYSTEM', NOW(), 'SYSTEM', FALSE),
    (gen_random_uuid(), 'OPPORTUNITY_READ',  'OPPORTUNITY', 'READ',   'SYSTEM', NOW(), 'SYSTEM', FALSE),
    (gen_random_uuid(), 'OPPORTUNITY_WRITE', 'OPPORTUNITY', 'WRITE',  'SYSTEM', NOW(), 'SYSTEM', FALSE),
    (gen_random_uuid(), 'OPPORTUNITY_DELETE','OPPORTUNITY', 'DELETE', 'SYSTEM', NOW(), 'SYSTEM', FALSE);

-- Insert default claims
INSERT INTO auth_claim (id, claim_code, claim_name, tenant_id, created_at, created_by_name, deleted)
VALUES
    (gen_random_uuid(), 'ADMIN',     'Full system administrator access', 'SYSTEM', NOW(), 'SYSTEM', FALSE),
    (gen_random_uuid(), 'MANAGER',   'Management level access',          'SYSTEM', NOW(), 'SYSTEM', FALSE),
    (gen_random_uuid(), 'SALES_REP', 'Sales representative access',      'SYSTEM', NOW(), 'SYSTEM', FALSE),
    (gen_random_uuid(), 'VIEWER',    'Read-only access',                 'SYSTEM', NOW(), 'SYSTEM', FALSE);

-- Insert default roles
INSERT INTO auth_role (id, role_code, role_name, description, is_seed, tenant_id, created_at, created_by_name, deleted)
VALUES
    (gen_random_uuid(), 'SUPER_ADMIN',   'Super Administrator',   'Full system access with all permissions',              TRUE,  'SYSTEM', NOW(), 'SYSTEM', FALSE),
    (gen_random_uuid(), 'ADMIN',         'Administrator',         'Administrative access to manage users and system',     TRUE,  'SYSTEM', NOW(), 'SYSTEM', FALSE),
    (gen_random_uuid(), 'SALES_MANAGER', 'Sales Manager',         'Manage sales team and access all CRM data',           FALSE, 'SYSTEM', NOW(), 'SYSTEM', FALSE),
    (gen_random_uuid(), 'SALES_REP',     'Sales Representative',  'Access to assigned customers and leads',              FALSE, 'SYSTEM', NOW(), 'SYSTEM', FALSE),
    (gen_random_uuid(), 'VIEWER',        'Viewer',                'Read-only access to CRM data',                        FALSE, 'SYSTEM', NOW(), 'SYSTEM', FALSE);

-- Assign all permissions to SUPER_ADMIN
INSERT INTO auth_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM auth_role r, auth_permission p
WHERE r.role_code = 'SUPER_ADMIN';

-- Assign user/role/permission management to ADMIN
INSERT INTO auth_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM auth_role r, auth_permission p
WHERE r.role_code = 'ADMIN'
  AND p.resource_name IN ('USER', 'ROLE', 'PERMISSION');

-- Assign all CRM permissions to SALES_MANAGER
INSERT INTO auth_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM auth_role r, auth_permission p
WHERE r.role_code = 'SALES_MANAGER'
  AND p.resource_name IN ('CRM', 'CUSTOMER', 'LEAD', 'OPPORTUNITY');

-- Assign read/write CRM permissions to SALES_REP (no delete)
INSERT INTO auth_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM auth_role r, auth_permission p
WHERE r.role_code = 'SALES_REP'
  AND p.resource_name IN ('CUSTOMER', 'LEAD', 'OPPORTUNITY')
  AND p.action_name IN ('READ', 'WRITE');

-- Assign read-only permissions to VIEWER
INSERT INTO auth_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM auth_role r, auth_permission p
WHERE r.role_code = 'VIEWER'
  AND p.action_name = 'READ';

-- Assign claims to roles
INSERT INTO auth_role_claim (role_id, claim_id)
SELECT r.id, c.id FROM auth_role r, auth_claim c WHERE r.role_code = 'SUPER_ADMIN'   AND c.claim_code = 'ADMIN';

INSERT INTO auth_role_claim (role_id, claim_id)
SELECT r.id, c.id FROM auth_role r, auth_claim c WHERE r.role_code = 'ADMIN'         AND c.claim_code = 'ADMIN';

INSERT INTO auth_role_claim (role_id, claim_id)
SELECT r.id, c.id FROM auth_role r, auth_claim c WHERE r.role_code = 'SALES_MANAGER' AND c.claim_code = 'MANAGER';

INSERT INTO auth_role_claim (role_id, claim_id)
SELECT r.id, c.id FROM auth_role r, auth_claim c WHERE r.role_code = 'SALES_REP'     AND c.claim_code = 'SALES_REP';

INSERT INTO auth_role_claim (role_id, claim_id)
SELECT r.id, c.id FROM auth_role r, auth_claim c WHERE r.role_code = 'VIEWER'        AND c.claim_code = 'VIEWER';

-- Create default admin user (password: admin123, BCrypt hash)
INSERT INTO auth_user (id, username, email, password_hash, status, full_name, tenant_id, created_at, created_by_name, deleted)
VALUES (
    gen_random_uuid(),
    'admin',
    'admin@company.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewfBzj6BnVfwzfHy',
    'ACTIVE',
    'System Administrator',
    'SYSTEM',
    NOW(),
    'SYSTEM',
    FALSE
);

-- Assign SUPER_ADMIN role to admin user
INSERT INTO auth_user_role (user_id, role_id)
SELECT u.id, r.id
FROM auth_user u, auth_role r
WHERE u.username = 'admin'
  AND r.role_code = 'SUPER_ADMIN';
