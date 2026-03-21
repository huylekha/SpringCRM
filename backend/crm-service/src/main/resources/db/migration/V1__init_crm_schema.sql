-- CRM Service Schema Migration
-- Compatible with PostgreSQL 16+

-- Create customer table
CREATE TABLE customer (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_code VARCHAR(50) NOT NULL UNIQUE,
    company_name VARCHAR(255) NOT NULL,
    contact_person VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    website VARCHAR(255),
    industry VARCHAR(100),
    company_size VARCHAR(50),
    annual_revenue DECIMAL(15,2),
    
    -- Address information
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    
    -- Business information
    tax_id VARCHAR(50),
    payment_terms VARCHAR(50),
    credit_limit DECIMAL(15,2),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    
    -- Relationship management
    assigned_sales_rep_id UUID,
    customer_since DATE,
    last_contact_date DATE,
    next_follow_up_date DATE,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Create lead table
CREATE TABLE lead (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_code VARCHAR(50) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    company VARCHAR(255),
    job_title VARCHAR(100),
    
    -- Lead source and qualification
    source VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    quality_score INTEGER CHECK (quality_score >= 0 AND quality_score <= 100),
    estimated_value DECIMAL(15,2),
    probability INTEGER CHECK (probability >= 0 AND probability <= 100),
    
    -- Address information
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    
    -- Relationship management
    assigned_sales_rep_id UUID,
    converted_to_customer_id UUID,
    conversion_date DATE,
    last_contact_date DATE,
    next_follow_up_date DATE,
    
    -- Notes and description
    description TEXT,
    notes TEXT,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT fk_lead_customer FOREIGN KEY (converted_to_customer_id) REFERENCES customer(id)
);

-- Create opportunity table
CREATE TABLE opportunity (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    opportunity_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    
    -- Financial information
    estimated_value DECIMAL(15,2) NOT NULL,
    probability INTEGER NOT NULL CHECK (probability >= 0 AND probability <= 100),
    expected_close_date DATE,
    actual_close_date DATE,
    
    -- Status and stage
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    stage VARCHAR(100) NOT NULL DEFAULT 'PROSPECTING',
    
    -- Relationships
    customer_id UUID NOT NULL,
    lead_id UUID,
    assigned_sales_rep_id UUID,
    
    -- Competition and notes
    competitors TEXT,
    win_loss_reason TEXT,
    notes TEXT,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT fk_opportunity_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
    CONSTRAINT fk_opportunity_lead FOREIGN KEY (lead_id) REFERENCES lead(id)
);

-- Create contact table (for customer contacts)
CREATE TABLE contact (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    mobile VARCHAR(50),
    job_title VARCHAR(100),
    department VARCHAR(100),
    
    -- Contact preferences
    is_primary BOOLEAN NOT NULL DEFAULT false,
    preferred_contact_method VARCHAR(50),
    
    -- Social and additional info
    linkedin_url VARCHAR(255),
    notes TEXT,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT fk_contact_customer FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE
);

-- Create activity table (interactions, calls, meetings, etc.)
CREATE TABLE activity (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(50) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    description TEXT,
    
    -- Timing
    scheduled_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    duration_minutes INTEGER,
    
    -- Status
    status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',
    outcome VARCHAR(100),
    
    -- Relationships (polymorphic - can be related to customer, lead, or opportunity)
    related_entity_type VARCHAR(50) NOT NULL, -- 'CUSTOMER', 'LEAD', 'OPPORTUNITY'
    related_entity_id UUID NOT NULL,
    contact_id UUID,
    
    -- Assignment
    assigned_to_user_id UUID,
    
    -- Notes and follow-up
    notes TEXT,
    follow_up_required BOOLEAN NOT NULL DEFAULT false,
    follow_up_date DATE,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT fk_activity_contact FOREIGN KEY (contact_id) REFERENCES contact(id)
);

-- Create indexes for performance
CREATE INDEX idx_customer_code ON customer(customer_code) WHERE deleted = false;
CREATE INDEX idx_customer_company_name ON customer(company_name) WHERE deleted = false;
CREATE INDEX idx_customer_status ON customer(status) WHERE deleted = false;
CREATE INDEX idx_customer_assigned_sales_rep ON customer(assigned_sales_rep_id) WHERE deleted = false;
CREATE INDEX idx_customer_created_at ON customer(created_at);

CREATE INDEX idx_lead_code ON lead(lead_code) WHERE deleted = false;
CREATE INDEX idx_lead_email ON lead(email) WHERE deleted = false;
CREATE INDEX idx_lead_status ON lead(status) WHERE deleted = false;
CREATE INDEX idx_lead_assigned_sales_rep ON lead(assigned_sales_rep_id) WHERE deleted = false;
CREATE INDEX idx_lead_created_at ON lead(created_at);

CREATE INDEX idx_opportunity_code ON opportunity(opportunity_code) WHERE deleted = false;
CREATE INDEX idx_opportunity_customer_id ON opportunity(customer_id) WHERE deleted = false;
CREATE INDEX idx_opportunity_status ON opportunity(status) WHERE deleted = false;
CREATE INDEX idx_opportunity_stage ON opportunity(stage) WHERE deleted = false;
CREATE INDEX idx_opportunity_assigned_sales_rep ON opportunity(assigned_sales_rep_id) WHERE deleted = false;
CREATE INDEX idx_opportunity_expected_close_date ON opportunity(expected_close_date);

CREATE INDEX idx_contact_customer_id ON contact(customer_id) WHERE deleted = false;
CREATE INDEX idx_contact_email ON contact(email) WHERE deleted = false;
CREATE INDEX idx_contact_is_primary ON contact(is_primary) WHERE deleted = false;

CREATE INDEX idx_activity_related_entity ON activity(related_entity_type, related_entity_id) WHERE deleted = false;
CREATE INDEX idx_activity_scheduled_at ON activity(scheduled_at) WHERE deleted = false;
CREATE INDEX idx_activity_assigned_to ON activity(assigned_to_user_id) WHERE deleted = false;
CREATE INDEX idx_activity_status ON activity(status) WHERE deleted = false;

-- Create updated_at trigger function (reuse from auth service)
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at
CREATE TRIGGER update_customer_updated_at BEFORE UPDATE ON customer FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_lead_updated_at BEFORE UPDATE ON lead FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_opportunity_updated_at BEFORE UPDATE ON opportunity FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_contact_updated_at BEFORE UPDATE ON contact FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_activity_updated_at BEFORE UPDATE ON activity FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();