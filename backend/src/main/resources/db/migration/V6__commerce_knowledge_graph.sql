-- V6: Commerce Knowledge Graph & Entity Relationships
CREATE TABLE IF NOT EXISTS commerce_relationships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID,
    source_type VARCHAR(64) NOT NULL,
    source_id VARCHAR(255) NOT NULL,
    relationship_type VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id VARCHAR(255) NOT NULL,
    weight NUMERIC(10, 4) DEFAULT 1.0,
    confidence NUMERIC(5, 4) DEFAULT 1.0,
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_graph_source ON commerce_relationships(source_type, source_id);
CREATE INDEX IF NOT EXISTS idx_graph_target ON commerce_relationships(target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_graph_rel_type ON commerce_relationships(relationship_type);
CREATE INDEX IF NOT EXISTS idx_graph_composite ON commerce_relationships(relationship_type, source_type, source_id);
CREATE INDEX IF NOT EXISTS idx_graph_tenant ON commerce_relationships(tenant_id);

-- Prevent exact duplicate directed edges by unique constraint
CREATE UNIQUE INDEX IF NOT EXISTS uq_graph_edge ON commerce_relationships(
    source_type, source_id, relationship_type, target_type, target_id
);
