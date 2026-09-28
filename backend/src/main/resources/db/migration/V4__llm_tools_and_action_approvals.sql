-- =============================================================================
-- V4__llm_tools_and_action_approvals.sql
-- LLM usage metrics, Agent Tools Registry, and Human-in-the-loop Action Approvals
-- =============================================================================

CREATE TABLE action_requests (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_name          VARCHAR(100)    NOT NULL DEFAULT 'AGENT',
    tool_name           VARCHAR(100)    NOT NULL,
    risk_level          VARCHAR(20)     NOT NULL DEFAULT 'HIGH',
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    input_payload       JSONB           NOT NULL DEFAULT '{}',
    execution_result    JSONB,
    rejection_reason    TEXT,
    requested_by        VARCHAR(255),
    approved_by         VARCHAR(255),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    resolved_at         TIMESTAMPTZ,
    CONSTRAINT chk_action_request_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'EXECUTED', 'FAILED')),
    CONSTRAINT chk_action_request_risk CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

CREATE INDEX idx_action_requests_status ON action_requests (status, created_at DESC);
CREATE INDEX idx_action_requests_tool ON action_requests (tool_name);

CREATE TABLE llm_usage_logs (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    provider            VARCHAR(50)     NOT NULL,
    model               VARCHAR(100)    NOT NULL,
    agent_name          VARCHAR(100),
    prompt_tokens       INT             NOT NULL DEFAULT 0,
    completion_tokens   INT             NOT NULL DEFAULT 0,
    total_tokens        INT             NOT NULL DEFAULT 0,
    latency_ms          BIGINT          NOT NULL DEFAULT 0,
    cost_usd            NUMERIC(10, 6)  NOT NULL DEFAULT 0.000000,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_llm_usage_logs_created_at ON llm_usage_logs (created_at DESC);
CREATE INDEX idx_llm_usage_logs_provider_model ON llm_usage_logs (provider, model);