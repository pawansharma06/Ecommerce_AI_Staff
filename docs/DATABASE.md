# ShopAI Database Architecture

## Database

PostgreSQL 16 + pgvector extension

## Performance Strategy

### pgvector — HNSW Indexes
- All vector similarity columns use HNSW indexes (better query performance than IVFFlat).
- Default parameters: m=16, ef_construction=64 (configurable via environment).
- Query parameter: `SET hnsw.ef_search = 40` before ANN queries.

### Relational Indexes
- B-tree on all foreign keys and frequently filtered columns.
- Composite indexes for multi-column WHERE patterns.
- Partial indexes for status-filtered queries.
- GIN indexes on JSONB metadata columns.
- BRIN indexes on append-only timestamp columns in high-volume tables.

### Table Partitioning
High-volume append-only tables use RANGE partitioning by `created_at` (monthly):
- `audit_logs` — every AI and admin action
- `conversation_messages` — all message content
- `webhook_events` — all Shopify webhook payloads
- `llm_usage` — all LLM API usage records

### PostgreSQL Configuration
| Parameter | Value |
|---|---|
| shared_buffers | 512MB |
| effective_cache_size | 1536MB |
| work_mem | 16MB |
| maintenance_work_mem | 256MB |
| max_connections | 200 |
| checkpoint_completion_target | 0.9 |
| wal_buffers | 16MB |
| default_statistics_target | 100 |
| random_page_cost | 1.1 (SSD) |
| max_parallel_workers_per_gather | 2 |

### HikariCP Connection Pool
| Parameter | Value |
|---|---|
| maximumPoolSize | 20 |
| minimumIdle | 5 |
| connectionTimeout | 30000ms |
| idleTimeout | 600000ms |
| maxLifetime | 1800000ms |

## Normalization

All tables follow 3NF:
- No repeating groups.
- All non-key attributes depend on the whole primary key.
- No transitive dependencies.
- JSONB used only for genuinely schemaless data (tool arguments, webhook payloads, metadata).

## Flyway Strategy

- All schema changes use Flyway versioned migrations.
- Migration naming: `V{N}__{description}.sql`
- Never modify existing migration files.
- Never delete data through migrations without explicit approval.

### Migration History

| Version | Description |
|---|---|
| V1 | Initial schema: tenants, shops, users, roles, permissions, audit_logs |

## Core Tables (V1)

### tenants
Top-level isolation boundary. Every record in the system belongs to a tenant.

### shops
A tenant can have multiple Shopify shops.

### users
Platform users. Belong to a tenant. Have roles.

### roles
Named role within a tenant (e.g., ADMIN, AGENT_OPERATOR, VIEWER).

### permissions
Fine-grained permission strings (e.g., product.read, order.update).

### user_roles
Many-to-many: users → roles.

### role_permissions
Many-to-many: roles → permissions.

### audit_logs (partitioned)
Every AI action, admin action, and security event.

## Future Tables (Later Phases)

| Table | Phase |
|---|---|
| shopify_installations | Phase 3 |
| agents, agent_versions, agent_tools | Phase 8 |
| conversations, conversation_messages | Phase 9 |
| customers, products, product_variants | Phase 4 |
| orders, order_items | Phase 4 |
| inventory, fulfillments, shipments | Phase 4 |
| commerce_relationships | Phase 5 |
| knowledge_sources, knowledge_documents, knowledge_chunks, knowledge_embeddings | Phase 6 |
| tool_definitions, tool_executions | Phase 8 |
| action_requests, action_approvals | Phase 8 |
| llm_providers, llm_usage | Phase 7 |
| webhook_events | Phase 3 |
