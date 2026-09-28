-- =============================================================================
-- ShopAI V5 Conversations and Agent Runtime Schema
-- Conversation sessions, turn history, and tool execution logs
-- =============================================================================

CREATE TABLE IF NOT EXISTS conversations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title           VARCHAR(255)    NOT NULL DEFAULT 'New Conversation',
    agent_type      VARCHAR(50)     NOT NULL DEFAULT 'CUSTOMER_SUPPORT', -- CUSTOMER_SUPPORT, ADMIN_COPILOT
    channel         VARCHAR(50)     NOT NULL DEFAULT 'WEB_CHAT',         -- WEB_CHAT, ADMIN_COPILOT, STOREFRONT_WIDGET
    customer_email  VARCHAR(255),
    customer_id     BIGINT,
    metadata        JSONB,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_conversations_customer_email ON conversations (customer_email);
CREATE INDEX IF NOT EXISTS idx_conversations_agent_type ON conversations (agent_type);
CREATE INDEX IF NOT EXISTS idx_conversations_updated_at ON conversations (updated_at DESC);

COMMENT ON TABLE conversations IS 'Conversation threads between human users / customers and autonomous AI agents.';

CREATE TABLE IF NOT EXISTS conversation_messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID            NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    role            VARCHAR(20)     NOT NULL, -- system, user, assistant, tool
    content         TEXT,
    tool_calls      JSONB,
    tool_call_id    VARCHAR(100),
    tool_name       VARCHAR(100),
    metadata        JSONB,
    token_count     INT             NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_conv_messages_conv_id ON conversation_messages (conversation_id, created_at ASC);

COMMENT ON TABLE conversation_messages IS 'Individual turn messages and tool call executions within a conversation thread.';
