-- =============================================================================
-- ShopAI Standalone Platform
-- Migration: V3__vector_rag_and_customer_memory_schema.sql
-- Description: Vector RAG, HNSW Embeddings, Knowledge Base, and Customer-Level Memory
-- =============================================================================

-- 1. Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. Knowledge Documents Table
-- Stores source documents (Products, Store Policies, FAQs, Order Summaries, Abandoned Carts, Custom Articles)
CREATE TABLE knowledge_documents (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    title           VARCHAR(500)    NOT NULL,
    content         TEXT            NOT NULL,
    document_type   VARCHAR(50)     NOT NULL, -- PRODUCT, POLICY, FAQ, ORDER_HISTORY, ABANDONED_CHECKOUT, CUSTOMER_PROFILE, CUSTOM
    source_id       VARCHAR(255),             -- e.g. shopify product ID, order number, or checkout ID
    customer_id     BIGINT,                   -- Null for general store knowledge; set for customer-scoped docs
    customer_email  VARCHAR(255),             -- Null for general store knowledge; set for customer-scoped docs
    status          VARCHAR(50)     NOT NULL DEFAULT 'INDEXED', -- INDEXED, PENDING, FAILED
    metadata        JSONB           DEFAULT '{}',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_kdocs_document_type ON knowledge_documents (document_type);
CREATE INDEX idx_kdocs_source_id ON knowledge_documents (source_id);
CREATE INDEX idx_kdocs_customer_email ON knowledge_documents (customer_email);
CREATE INDEX idx_kdocs_metadata ON knowledge_documents USING gin (metadata);

-- 3. Knowledge Chunks Table
-- Vectorized text chunks with 1536-dimensional embeddings (compatible with text-embedding-3-small, Ada-002, and Gemini)
CREATE TABLE knowledge_chunks (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id     UUID            NOT NULL REFERENCES knowledge_documents(id) ON DELETE CASCADE,
    chunk_index     INT             NOT NULL DEFAULT 0,
    content         TEXT            NOT NULL,
    token_count     INT             NOT NULL DEFAULT 0,
    embedding       vector(1536),
    customer_id     BIGINT,
    customer_email  VARCHAR(255),
    metadata        JSONB           DEFAULT '{}',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_kchunks_document_id ON knowledge_chunks (document_id);
CREATE INDEX idx_kchunks_customer_email ON knowledge_chunks (customer_email);
CREATE INDEX idx_kchunks_metadata ON knowledge_chunks USING gin (metadata);
CREATE INDEX idx_kchunks_content_tsv ON knowledge_chunks USING gin (to_tsvector('english', content));

-- HNSW Vector Cosine Distance Index on knowledge_chunks
CREATE INDEX idx_kchunks_embedding_hnsw 
ON knowledge_chunks USING hnsw (embedding vector_cosine_ops) 
WITH (m = 16, ef_construction = 64);

-- 4. Customer Semantic Memories Table
-- Stores long-term personalized customer preferences, size/brand affinities, notes, feedback, and facts
CREATE TABLE customer_memories (
    id                UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id       BIGINT,
    customer_email    VARCHAR(255)    NOT NULL,
    category          VARCHAR(50)     NOT NULL DEFAULT 'PREFERENCE', -- PREFERENCE, ORDER_FACT, ADDRESS_FACT, FEEDBACK, INTERACTION
    memory_key        VARCHAR(255)    NOT NULL,                      -- e.g. "preferred_size", "shipping_instructions", "brand_affinity"
    memory_value      TEXT            NOT NULL,
    confidence_score  NUMERIC(3, 2)   NOT NULL DEFAULT 1.00,
    embedding         vector(1536),
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cmem_customer_email ON customer_memories (customer_email);
CREATE INDEX idx_cmem_category ON customer_memories (category);
CREATE INDEX idx_cmem_key ON customer_memories (memory_key);

-- HNSW Vector Cosine Distance Index on customer_memories
CREATE INDEX idx_cmem_embedding_hnsw 
ON customer_memories USING hnsw (embedding vector_cosine_ops) 
WITH (m = 16, ef_construction = 64);
