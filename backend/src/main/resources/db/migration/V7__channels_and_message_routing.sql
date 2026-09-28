-- V7: Multi-Channel Commerce Engine (WhatsApp, Email & Message Routing)
CREATE TABLE IF NOT EXISTS channel_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID,
    channel_type VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    auto_reply_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    config_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_channel_type UNIQUE (channel_type)
);

CREATE TABLE IF NOT EXISTS channel_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID,
    channel_type VARCHAR(64) NOT NULL,
    direction VARCHAR(32) NOT NULL, -- INBOUND, OUTBOUND
    external_message_id VARCHAR(255),
    conversation_id UUID REFERENCES conversations(id) ON DELETE SET NULL,
    sender_id VARCHAR(255) NOT NULL, -- Phone number, email address, or user ID
    sender_name VARCHAR(255),
    recipient_id VARCHAR(255) NOT NULL,
    subject VARCHAR(500),
    content TEXT NOT NULL,
    raw_payload JSONB DEFAULT '{}'::jsonb,
    status VARCHAR(32) NOT NULL DEFAULT 'RECEIVED', -- RECEIVED, PROCESSING, SENT, FAILED, PENDING_APPROVAL
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_channel_messages_channel ON channel_messages(channel_type);
CREATE INDEX IF NOT EXISTS idx_channel_messages_sender ON channel_messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_channel_messages_conv ON channel_messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_channel_messages_created ON channel_messages(created_at DESC);
