-- =============================================================================
-- ShopAI V1 Initial Schema (Standalone Architecture)
-- PostgreSQL 16 + pgvector
-- Single-Store Dedicated Platform with Shopify Custom App Integration
-- =============================================================================

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS "vector";

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================================================
-- SHOPIFY_CONFIG
-- Stores the single connected Shopify Custom/Private App credentials & settings
-- =============================================================================
CREATE TABLE shopify_config (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_domain             VARCHAR(255)        NOT NULL,
    access_token_encrypted  TEXT                NOT NULL,
    webhook_secret_encrypted TEXT,
    api_version             VARCHAR(20)         NOT NULL DEFAULT '2024-04',
    shop_name               VARCHAR(255),
    shop_owner              VARCHAR(255),
    email                   VARCHAR(255),
    currency                VARCHAR(10)         DEFAULT 'USD',
    timezone                VARCHAR(100)        DEFAULT 'UTC',
    status                  VARCHAR(20)         NOT NULL DEFAULT 'CONFIGURED',
    metadata                JSONB,
    created_at              TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    CONSTRAINT shopify_config_domain_unique UNIQUE (shop_domain),
    CONSTRAINT shopify_config_status_check CHECK (status IN ('CONFIGURED', 'CONNECTED', 'DISCONNECTED', 'ERROR'))
);

CREATE INDEX idx_shopify_config_domain ON shopify_config (shop_domain);
CREATE INDEX idx_shopify_config_status ON shopify_config (status);
COMMENT ON TABLE shopify_config IS 'Stores the single store custom/private app configuration and encrypted credentials.';

-- =============================================================================
-- USERS
-- Local platform users with RBAC.
-- =============================================================================
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255)    NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    first_name      VARCHAR(100),
    last_name       VARCHAR(100),
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    email_verified  BOOLEAN         NOT NULL DEFAULT TRUE,
    locale          VARCHAR(20)     NOT NULL DEFAULT 'en',
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT users_email_unique UNIQUE (email),
    CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED', 'PENDING'))
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_status ON users (status);
COMMENT ON TABLE users IS 'Platform users for the standalone application. Authenticated via Spring Security.';

-- =============================================================================
-- ROLES
-- Named roles (e.g., ADMIN, OPERATOR, VIEWER).
-- =============================================================================
CREATE TABLE roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100)    NOT NULL,
    description VARCHAR(500),
    is_system   BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT roles_name_unique UNIQUE (name)
);

CREATE INDEX idx_roles_name ON roles (name);
COMMENT ON TABLE roles IS 'Named roles in the system. System roles cannot be deleted.';

-- =============================================================================
-- PERMISSIONS
-- Fine-grained permission strings.
-- =============================================================================
CREATE TABLE permissions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100)    NOT NULL,
    description VARCHAR(500),
    module      VARCHAR(100)    NOT NULL,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT permissions_name_unique UNIQUE (name)
);

CREATE INDEX idx_permissions_module ON permissions (module);
COMMENT ON TABLE permissions IS 'Fine-grained permission strings for method security.';

-- Seed system permissions
INSERT INTO permissions (name, description, module) VALUES
    ('product.read',       'View products',              'product'),
    ('product.create',     'Create products',            'product'),
    ('product.update',     'Update products',            'product'),
    ('product.delete',     'Delete products',            'product'),
    ('order.read',         'View orders',                'order'),
    ('order.update',       'Update orders',              'order'),
    ('order.cancel',       'Cancel orders',              'order'),
    ('inventory.read',     'View inventory',             'inventory'),
    ('inventory.update',   'Update inventory',           'inventory'),
    ('customer.read',      'View customers',             'customer'),
    ('report.read',        'View reports',               'reporting'),
    ('report.execute',     'Execute reports',            'reporting'),
    ('agent.execute',      'Execute AI agents',          'agent'),
    ('integration.manage', 'Manage Shopify integration', 'integration'),
    ('knowledge.manage',   'Manage knowledge base',      'knowledge'),
    ('audit.read',         'View audit logs',            'audit'),
    ('settings.manage',    'Manage system settings',     'settings');

-- =============================================================================
-- USER_ROLES
-- Many-to-many: users to roles.
-- =============================================================================
CREATE TABLE user_roles (
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id     UUID        NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);

-- =============================================================================
-- ROLE_PERMISSIONS
-- Many-to-many: roles to permissions.
-- =============================================================================
CREATE TABLE role_permissions (
    role_id       UUID        NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID        NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    granted_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (role_id, permission_id)
);

CREATE INDEX idx_role_permissions_permission_id ON role_permissions (permission_id);

-- Seed System Roles
INSERT INTO roles (id, name, description, is_system) VALUES
    ('11111111-1111-1111-1111-111111111111', 'ADMIN', 'System Administrator with full access', TRUE),
    ('22222222-2222-2222-2222-222222222222', 'OPERATOR', 'Commerce and AI Agent Operator', TRUE),
    ('33333333-3333-3333-3333-333333333333', 'VIEWER', 'Read-only access', TRUE);

-- Map all permissions to ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT '11111111-1111-1111-1111-111111111111', id FROM permissions;

-- Map operator permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT '22222222-2222-2222-2222-222222222222', id FROM permissions
WHERE name NOT IN ('audit.read', 'settings.manage', 'integration.manage');

-- Map viewer permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT '33333333-3333-3333-3333-333333333333', id FROM permissions
WHERE name LIKE '%.read';

-- =============================================================================
-- WEBHOOK_EVENTS (Range-partitioned by created_at)
-- Ingested Shopify webhook notifications for asynchronous reliable processing
-- =============================================================================
CREATE TABLE webhook_events (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    topic               VARCHAR(100)    NOT NULL,
    shopify_webhook_id  VARCHAR(100),
    shop_domain         VARCHAR(255)    NOT NULL,
    api_version         VARCHAR(20)     NOT NULL,
    payload             JSONB           NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    attempts            INT             NOT NULL DEFAULT 0,
    error_message       TEXT,
    processed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT webhook_events_status_check CHECK (status IN ('PENDING', 'PROCESSED', 'FAILED', 'RETRYING')),
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- Create initial partitions (2024-2027)
CREATE TABLE webhook_events_2024_q4 PARTITION OF webhook_events
    FOR VALUES FROM ('2024-10-01') TO ('2025-01-01');
CREATE TABLE webhook_events_2025_q1 PARTITION OF webhook_events
    FOR VALUES FROM ('2025-01-01') TO ('2025-04-01');
CREATE TABLE webhook_events_2025_q2 PARTITION OF webhook_events
    FOR VALUES FROM ('2025-04-01') TO ('2025-07-01');
CREATE TABLE webhook_events_2025_q3 PARTITION OF webhook_events
    FOR VALUES FROM ('2025-07-01') TO ('2025-10-01');
CREATE TABLE webhook_events_2025_q4 PARTITION OF webhook_events
    FOR VALUES FROM ('2025-10-01') TO ('2026-01-01');
CREATE TABLE webhook_events_2026_q1 PARTITION OF webhook_events
    FOR VALUES FROM ('2026-01-01') TO ('2026-04-01');
CREATE TABLE webhook_events_2026_q2 PARTITION OF webhook_events
    FOR VALUES FROM ('2026-04-01') TO ('2026-07-01');
CREATE TABLE webhook_events_2026_q3 PARTITION OF webhook_events
    FOR VALUES FROM ('2026-07-01') TO ('2026-10-01');
CREATE TABLE webhook_events_2026_q4 PARTITION OF webhook_events
    FOR VALUES FROM ('2026-10-01') TO ('2027-01-01');
CREATE TABLE webhook_events_2027_q1 PARTITION OF webhook_events
    FOR VALUES FROM ('2027-01-01') TO ('2027-04-01');

CREATE INDEX idx_webhook_events_topic ON webhook_events (topic, created_at);
CREATE INDEX idx_webhook_events_status ON webhook_events (status, created_at);
CREATE INDEX idx_webhook_events_webhook_id ON webhook_events (shopify_webhook_id, created_at);
COMMENT ON TABLE webhook_events IS 'Shopify webhook event log with idempotency tracking and async processing.';

-- =============================================================================
-- AUDIT_LOGS (Range-partitioned by created_at)
-- Full security, tool, and AI agent execution audit trail. Append-only.
-- =============================================================================
CREATE TABLE audit_logs (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id         UUID,
    agent_id        UUID,
    conversation_id UUID,
    request_id      VARCHAR(100),
    tool            VARCHAR(200),
    action          VARCHAR(200)    NOT NULL,
    entity_type     VARCHAR(100),
    entity_id       VARCHAR(255),
    before_state    JSONB,
    after_state     JSONB,
    arguments       JSONB,
    approval_status VARCHAR(20),
    execution_status VARCHAR(20)    NOT NULL DEFAULT 'SUCCESS',
    error_message   TEXT,
    ip_address      INET,
    user_agent      TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT audit_logs_approval_check CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED', NULL)),
    CONSTRAINT audit_logs_exec_status_check CHECK (execution_status IN ('SUCCESS', 'FAILURE', 'PENDING', 'SKIPPED')),
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- Create initial partitions (2024-2027)
CREATE TABLE audit_logs_2024_q4 PARTITION OF audit_logs
    FOR VALUES FROM ('2024-10-01') TO ('2025-01-01');
CREATE TABLE audit_logs_2025_q1 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-01-01') TO ('2025-04-01');
CREATE TABLE audit_logs_2025_q2 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-04-01') TO ('2025-07-01');
CREATE TABLE audit_logs_2025_q3 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-07-01') TO ('2025-10-01');
CREATE TABLE audit_logs_2025_q4 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-10-01') TO ('2026-01-01');
CREATE TABLE audit_logs_2026_q1 PARTITION OF audit_logs
    FOR VALUES FROM ('2026-01-01') TO ('2026-04-01');
CREATE TABLE audit_logs_2026_q2 PARTITION OF audit_logs
    FOR VALUES FROM ('2026-04-01') TO ('2026-07-01');
CREATE TABLE audit_logs_2026_q3 PARTITION OF audit_logs
    FOR VALUES FROM ('2026-07-01') TO ('2026-10-01');
CREATE TABLE audit_logs_2026_q4 PARTITION OF audit_logs
    FOR VALUES FROM ('2026-10-01') TO ('2027-01-01');
CREATE TABLE audit_logs_2027_q1 PARTITION OF audit_logs
    FOR VALUES FROM ('2027-01-01') TO ('2027-04-01');

CREATE INDEX idx_audit_logs_user_id ON audit_logs (user_id, created_at) WHERE user_id IS NOT NULL;
CREATE INDEX idx_audit_logs_agent_id ON audit_logs (agent_id, created_at) WHERE agent_id IS NOT NULL;
CREATE INDEX idx_audit_logs_action ON audit_logs (action, created_at);
CREATE INDEX idx_audit_logs_exec_status ON audit_logs (execution_status, created_at);
COMMENT ON TABLE audit_logs IS 'Append-only audit log. Partitioned quarterly for query performance. Never store credentials.';
