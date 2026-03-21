-- Seed Sample CRM Data
-- Compatible with PostgreSQL 16+

-- Insert sample customers
INSERT INTO customer (id, customer_code, company_name, contact_person, email, phone, industry, company_size, address_line1, city, state, country, status, created_by) VALUES
    (gen_random_uuid(), 'CUST-001', 'Acme Corporation', 'John Smith', 'john.smith@acme.com', '+1-555-0101', 'Technology', 'Large', '123 Business Ave', 'New York', 'NY', 'USA', 'ACTIVE', 'SYSTEM'),
    (gen_random_uuid(), 'CUST-002', 'Global Solutions Inc', 'Sarah Johnson', 'sarah.j@globalsolutions.com', '+1-555-0102', 'Consulting', 'Medium', '456 Corporate Blvd', 'Los Angeles', 'CA', 'USA', 'ACTIVE', 'SYSTEM'),
    (gen_random_uuid(), 'CUST-003', 'Tech Innovators Ltd', 'Mike Chen', 'mike.chen@techinnovators.com', '+1-555-0103', 'Software', 'Small', '789 Innovation Dr', 'San Francisco', 'CA', 'USA', 'ACTIVE', 'SYSTEM'),
    (gen_random_uuid(), 'CUST-004', 'Manufacturing Plus', 'Lisa Rodriguez', 'lisa.r@mfgplus.com', '+1-555-0104', 'Manufacturing', 'Large', '321 Industrial Way', 'Chicago', 'IL', 'USA', 'ACTIVE', 'SYSTEM'),
    (gen_random_uuid(), 'CUST-005', 'Retail Excellence', 'David Wilson', 'david.wilson@retailexcellence.com', '+1-555-0105', 'Retail', 'Medium', '654 Commerce St', 'Miami', 'FL', 'USA', 'ACTIVE', 'SYSTEM');

-- Insert sample leads
INSERT INTO lead (id, lead_code, first_name, last_name, email, phone, company, job_title, source, status, quality_score, estimated_value, probability, description, created_by) VALUES
    (gen_random_uuid(), 'LEAD-001', 'Robert', 'Brown', 'robert.brown@prospect1.com', '+1-555-0201', 'Prospect Company 1', 'CTO', 'Website', 'QUALIFIED', 85, 50000.00, 70, 'Interested in our enterprise solution', 'SYSTEM'),
    (gen_random_uuid(), 'LEAD-002', 'Jennifer', 'Davis', 'jennifer.davis@prospect2.com', '+1-555-0202', 'Prospect Company 2', 'VP Sales', 'Referral', 'CONTACTED', 75, 75000.00, 60, 'Looking for CRM integration', 'SYSTEM'),
    (gen_random_uuid(), 'LEAD-003', 'Michael', 'Garcia', 'michael.garcia@prospect3.com', '+1-555-0203', 'Prospect Company 3', 'CEO', 'Trade Show', 'NEW', 90, 100000.00, 80, 'High-value prospect from trade show', 'SYSTEM'),
    (gen_random_uuid(), 'LEAD-004', 'Amanda', 'Martinez', 'amanda.martinez@prospect4.com', '+1-555-0204', 'Prospect Company 4', 'Director IT', 'Cold Call', 'NURTURING', 60, 30000.00, 40, 'Needs more information about pricing', 'SYSTEM'),
    (gen_random_uuid(), 'LEAD-005', 'James', 'Anderson', 'james.anderson@prospect5.com', '+1-555-0205', 'Prospect Company 5', 'Manager', 'LinkedIn', 'QUALIFIED', 70, 45000.00, 65, 'Ready for demo presentation', 'SYSTEM');

-- Insert sample opportunities
INSERT INTO opportunity (id, opportunity_code, name, description, estimated_value, probability, expected_close_date, status, stage, customer_id, notes, created_by) VALUES
    (gen_random_uuid(), 'OPP-001', 'Acme Corp - Enterprise License', 'Annual enterprise software license for 500 users', 150000.00, 80, CURRENT_DATE + INTERVAL '30 days', 'OPEN', 'NEGOTIATION', (SELECT id FROM customer WHERE customer_code = 'CUST-001'), 'Contract terms under review', 'SYSTEM'),
    (gen_random_uuid(), 'OPP-002', 'Global Solutions - Consulting Package', 'Implementation and consulting services', 85000.00, 65, CURRENT_DATE + INTERVAL '45 days', 'OPEN', 'PROPOSAL', (SELECT id FROM customer WHERE customer_code = 'CUST-002'), 'Proposal submitted, awaiting response', 'SYSTEM'),
    (gen_random_uuid(), 'OPP-003', 'Tech Innovators - Custom Development', 'Custom module development and integration', 120000.00, 90, CURRENT_DATE + INTERVAL '60 days', 'OPEN', 'VERBAL_COMMITMENT', (SELECT id FROM customer WHERE customer_code = 'CUST-003'), 'Verbal agreement reached, preparing contract', 'SYSTEM'),
    (gen_random_uuid(), 'OPP-004', 'Manufacturing Plus - System Upgrade', 'Upgrade to latest version with additional features', 75000.00, 70, CURRENT_DATE + INTERVAL '90 days', 'OPEN', 'EVALUATION', (SELECT id FROM customer WHERE customer_code = 'CUST-004'), 'Technical evaluation in progress', 'SYSTEM'),
    (gen_random_uuid(), 'OPP-005', 'Retail Excellence - Multi-store License', 'Software license for 25 retail locations', 200000.00, 85, CURRENT_DATE + INTERVAL '120 days', 'OPEN', 'NEGOTIATION', (SELECT id FROM customer WHERE customer_code = 'CUST-005'), 'Negotiating volume discount', 'SYSTEM');

-- Insert sample contacts for customers
INSERT INTO contact (id, customer_id, first_name, last_name, email, phone, job_title, department, is_primary, created_by) VALUES
    (gen_random_uuid(), (SELECT id FROM customer WHERE customer_code = 'CUST-001'), 'John', 'Smith', 'john.smith@acme.com', '+1-555-0101', 'CEO', 'Executive', true, 'SYSTEM'),
    (gen_random_uuid(), (SELECT id FROM customer WHERE customer_code = 'CUST-001'), 'Jane', 'Doe', 'jane.doe@acme.com', '+1-555-0111', 'CTO', 'Technology', false, 'SYSTEM'),
    (gen_random_uuid(), (SELECT id FROM customer WHERE customer_code = 'CUST-002'), 'Sarah', 'Johnson', 'sarah.j@globalsolutions.com', '+1-555-0102', 'VP Operations', 'Operations', true, 'SYSTEM'),
    (gen_random_uuid(), (SELECT id FROM customer WHERE customer_code = 'CUST-003'), 'Mike', 'Chen', 'mike.chen@techinnovators.com', '+1-555-0103', 'Founder', 'Executive', true, 'SYSTEM'),
    (gen_random_uuid(), (SELECT id FROM customer WHERE customer_code = 'CUST-004'), 'Lisa', 'Rodriguez', 'lisa.r@mfgplus.com', '+1-555-0104', 'Plant Manager', 'Operations', true, 'SYSTEM'),
    (gen_random_uuid(), (SELECT id FROM customer WHERE customer_code = 'CUST-005'), 'David', 'Wilson', 'david.wilson@retailexcellence.com', '+1-555-0105', 'Regional Manager', 'Sales', true, 'SYSTEM');

-- Insert sample activities
INSERT INTO activity (id, type, subject, description, scheduled_at, status, related_entity_type, related_entity_id, outcome, notes, created_by) VALUES
    (gen_random_uuid(), 'CALL', 'Initial Discovery Call', 'First call to understand requirements', CURRENT_TIMESTAMP - INTERVAL '5 days', 'COMPLETED', 'CUSTOMER', (SELECT id FROM customer WHERE customer_code = 'CUST-001'), 'POSITIVE', 'Customer is very interested in our solution', 'SYSTEM'),
    (gen_random_uuid(), 'MEETING', 'Product Demo', 'Demonstrate key features and capabilities', CURRENT_TIMESTAMP - INTERVAL '3 days', 'COMPLETED', 'OPPORTUNITY', (SELECT id FROM opportunity WHERE opportunity_code = 'OPP-001'), 'POSITIVE', 'Demo went very well, customer impressed', 'SYSTEM'),
    (gen_random_uuid(), 'EMAIL', 'Follow-up Proposal', 'Send detailed proposal and pricing', CURRENT_TIMESTAMP - INTERVAL '1 day', 'COMPLETED', 'OPPORTUNITY', (SELECT id FROM opportunity WHERE opportunity_code = 'OPP-002'), 'NEUTRAL', 'Proposal sent, waiting for feedback', 'SYSTEM'),
    (gen_random_uuid(), 'CALL', 'Technical Discussion', 'Discuss integration requirements', CURRENT_TIMESTAMP + INTERVAL '2 days', 'SCHEDULED', 'LEAD', (SELECT id FROM lead WHERE lead_code = 'LEAD-001'), NULL, 'Scheduled call to discuss technical details', 'SYSTEM'),
    (gen_random_uuid(), 'MEETING', 'Contract Review', 'Review contract terms and conditions', CURRENT_TIMESTAMP + INTERVAL '1 week', 'SCHEDULED', 'OPPORTUNITY', (SELECT id FROM opportunity WHERE opportunity_code = 'OPP-003'), NULL, 'Final contract review meeting', 'SYSTEM');

-- Update some leads with follow-up dates
UPDATE lead SET 
    next_follow_up_date = CURRENT_DATE + INTERVAL '3 days',
    last_contact_date = CURRENT_DATE - INTERVAL '2 days'
WHERE lead_code IN ('LEAD-001', 'LEAD-002', 'LEAD-005');

-- Update some customers with follow-up dates
UPDATE customer SET 
    last_contact_date = CURRENT_DATE - INTERVAL '1 week',
    next_follow_up_date = CURRENT_DATE + INTERVAL '2 weeks'
WHERE customer_code IN ('CUST-001', 'CUST-003', 'CUST-005');