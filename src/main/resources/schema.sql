-- 1. Employees Table
CREATE TABLE IF NOT EXISTS employees (
    id UUID PRIMARY KEY,
    employee_code VARCHAR(50) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    iban VARCHAR(34) NOT NULL,
    bic VARCHAR(11) NOT NULL,
    tax_class VARCHAR(10) NOT NULL,
    base_salary NUMERIC(12, 2) NOT NULL,
    country_code VARCHAR(5) NOT NULL DEFAULT 'DE',
    currency VARCHAR(3) DEFAULT 'EUR',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Payroll Batches Table
CREATE TABLE IF NOT EXISTS payroll_batches (
    id UUID PRIMARY KEY,
    batch_reference VARCHAR(100) UNIQUE NOT NULL,
    payroll_period VARCHAR(7) NOT NULL,
    total_gross NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    total_net NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    total_tax NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL,
    idempotency_key VARCHAR(128) UNIQUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Payroll Line Items Table
CREATE TABLE IF NOT EXISTS payroll_items (
    id UUID PRIMARY KEY,
    batch_id UUID NOT NULL REFERENCES payroll_batches(id),
    employee_id UUID NOT NULL REFERENCES employees(id),
    gross_salary NUMERIC(12, 2) NOT NULL,
    tax_deductions NUMERIC(12, 2) NOT NULL,
    social_security_deductions NUMERIC(12, 2) NOT NULL,
    net_salary NUMERIC(12, 2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    transaction_reference VARCHAR(100) UNIQUE,
    sepa_msg_id VARCHAR(100),
    processed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_batch_employee UNIQUE (batch_id, employee_id)
);

-- 4. Immutable Financial Audit Logs Table
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY,
    entity_name VARCHAR(50) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    payload_json TEXT NOT NULL,
    hmac_signature VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
