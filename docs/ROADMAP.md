# ShopAI Development Roadmap

## Phases

| Phase | Name | Status |
|---|---|---|
| 1 | Infrastructure | ✅ Completed |
| 2 | Security (Auth + RBAC) | ✅ Completed |
| 3 | Shopify Integration | ✅ Completed |
| 4 | Commerce Data Sync | ✅ Completed |
| 5 | Commerce Graph | ✅ Completed |
| 6 | Knowledge Base + Vector RAG | ✅ Completed |
| 7 | LLM Provider Abstraction | ✅ Completed |
| 8 | Agent Tools + Registry | ✅ Completed |
| 9 | Customer Agent | ✅ Completed |
| 10 | Admin Agent | ✅ Completed |
| 11 | WhatsApp Channel | ✅ Completed |
| 12 | Email Channel | ✅ Completed |
| 13 | Analytics + Reporting | ✅ Completed |
| 14 | Agent Playground | ✅ Completed |
| 15 | Production Hardening | ✅ Completed |
| 16 | Open-Source Release | ✅ Completed |

## Phase Details

### Phase 1 — Infrastructure
- Repository structure
- Docker Compose (Nginx, Backend, Frontend, PostgreSQL+pgvector, Redis)
- Spring Boot skeleton
- React skeleton
- Flyway migrations
- Health endpoint
- Automated tests

### Phase 2 — Security
- Spring Security
- JWT authentication
- User management
- Role management
- Permission management
- RBAC enforcement
- Tenant management
- Tenant isolation tests

### Phase 3 — Shopify Integration
- Shopify OAuth flow
- ShopifyClient + ShopifyGraphQLClient
- Shopify installation management
- Webhook controller + HMAC verification
- Webhook event persistence
- Async webhook processing

### Phase 4 — Commerce Data Sync
- Product sync
- Product variant sync
- Collection sync
- Customer sync
- Order sync
- Inventory sync
- Fulfillment sync

### Phase 5 — Commerce Graph
- commerce_relationships table
- GraphRepository interface
- PostgresGraphRepository implementation
- GraphRetriever with controlled query methods
- Graph Explorer UI

### Phase 6 — Knowledge Base + Vector RAG
- knowledge_sources, knowledge_documents, knowledge_chunks, knowledge_embeddings
- Document parsers (TXT, Markdown, PDF, HTML)
- Chunking pipeline
- Embedding pipeline (via LLMProvider)
- HNSW index on embeddings
- VectorRetriever

### Phase 7 — LLM Provider Abstraction
- LLMProvider interface
- OpenAIProvider
- AnthropicProvider
- GoogleProvider
- XAIProvider
- OpenAICompatibleProvider
- LLM usage tracking

### Phase 8 — Agent Tools + Registry
- ToolRegistry
- Tool metadata (permission, riskLevel, requiresApproval)
- Tool execution engine
- Action approval flow (HIGH/CRITICAL)
- Audit logging for tool executions

### Phase 9 — Customer Agent
- Customer agent persona
- Customer agent tool set
- Conversation engine
- Context window management
- ContextEngine (Graph + Vector + Shopify fusion)

### Phase 10 — Admin Agent
- Admin agent persona
- Admin agent tool set
- Admin approval dashboard

### Phase 11 — WhatsApp Channel
- WhatsApp Cloud API webhook
- WhatsApp adapter
- WhatsApp → Conversation Engine

### Phase 12 — Email Channel
- SMTP/IMAP/Gmail support
- Email thread resolution
- Email → Conversation Engine
- Human approval before send

### Phase 13 — Analytics + Reporting
- Sales analytics
- Product analytics
- Customer analytics
- AI usage analytics
- Natural language reporting

### Phase 14 — Agent Playground
- Debug interface
- Intent visualization
- Context display
- Tool call tracing
- Token usage display

### Phase 15 — Production Hardening
- TLS/HTTPS
- Rate limiting
- Prometheus + Grafana
- OpenTelemetry
- Load testing

### Phase 16 — Open-Source Release
- Documentation review
- Security audit
- Public GitHub release
- Docker Hub images