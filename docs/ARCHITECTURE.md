# System Architecture & Technical Specification

## 1. High-Level Architecture Overview

Ecommerce AI Staff is built as a modular, high-throughput, multi-tenant AI workforce platform designed around safety, determinism, and low latency.

```
+-----------------------------------------------------------------------------------+
|                                Customer Channels                                  |
|         [ Webchat Widget ]       [ WhatsApp Business API ]       [ Email / SMTP ]  |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                        Edge Layer (Nginx Reverse Proxy)                           |
+-----------------------------------------------------------------------------------+
                     |                                         |
                     v                                         v
+----------------------------------+       +----------------------------------------+
|      Frontend Application        |       |        Backend Core (Spring Boot 3.3)  |
|  - React 18 + TypeScript + Vite  |       |  - Java 21 LTS + Spring Security (JWT) |
|  - Material UI (MUI v5)          |       |  - Multi-Tenant Security Interceptors  |
|  - TanStack React Query v5       |       |  - Reactive WebSocket / SSE Streaming  |
+----------------------------------+       +----------------------------------------+
                                                               |
    +----------------------------------------------------------+----------------------------------------------------------+
    |                                                          |                                                          |
    v                                                          v                                                          v
+-----------------------------+             +-----------------------------+             +-----------------------------+
|      AI Agent Subsystem     |             |      Data & RAG Subsystem   |             |   Store Connector Subsystem |
| - Reasoning & Planner Engine|             | - Vector RAG (pgvector HNSW)|             | - Shopify Admin GraphQL     |
| - Tool Registry (Safe Tools)|             | - Graph RAG (Entity Nodes)  |             | - Shopify Webhooks Manager  |
| - Action Approval Workflow  |             | - Document Ingestion & Chunk|             | - WooCommerce REST v3       |
| - Multi-LLM Provider Engine |             | - Redis Semantic Cache      |             | - WooCommerce Webhooks      |
+-----------------------------+             +-----------------------------+             +-----------------------------+
```

---

## 2. Core Subsystems

### 2.1 AI Agent & Planner Subsystem
- **Reasoning Loop**: The agent operates on a ReAct (Reasoning + Acting) loop. Upon receiving an inbound query from any channel, the Planner analyzes user intent, searches knowledge contexts, and determines if tool calls are required.
- **Tool Registry**: All external interactions (e.g. `search_products`, `get_order_status`, `check_inventory`, `calculate_shipping`) are implemented as typed, annotated Java classes implementing the `AgentTool` interface.
- **Safety & Approvals (Human-in-the-loop)**: Any destructive or monetary action (e.g. `issue_refund`, `cancel_order`, `update_inventory`, `generate_discount_code`) triggers an `ActionRequest` in the `PENDING` state and requires merchant operator approval.

### 2.2 Multi-LLM Provider Subsystem
- Provider-agnostic abstraction through `LlmProvider` interface.
- **OpenAI Provider**: Supports GPT-4o, GPT-4o-mini, GPT-4-turbo with native tool calling and function schemas.
- **Google Gemini Provider**: Supports Gemini 1.5 Pro and Flash with multimodal vision and context windows.
- **Local Fallback**: Local Ollama or deterministic offline fallback when external LLM endpoints are unreachable.
- **Multimodal Support**:
  - **Vision**: Analyzes product photos for defect detection, visual search, and color/material identification.
  - **Speech**: Whisper-based Speech-to-Text (STT) and dynamic Text-to-Speech (TTS).

### 2.3 Hybrid RAG & Knowledge Subsystem
- **Vector RAG**: Uses PostgreSQL with the `pgvector` extension and Hierarchical Navigable Small World (`HNSW`) index for fast, low-latency approximate nearest neighbor (ANN) cosine similarity search.
- **Graph RAG**: Models entity relationships between `Customer`, `Order`, `Product`, `Category`, and `Review` to navigate multi-hop queries (e.g. "Find what customers who bought item X usually return").
- **Knowledge Documents**: Automated parsing and chunking for store policies (returns, shipping, warranties, FAQs).

### 2.4 Store Connectors (Shopify & WooCommerce)
- **Shopify**: Custom Private App integration using Shopify Admin GraphQL API (2024-10+) and HMAC-verified webhook listeners.
- **WooCommerce**: REST API v3 client with Consumer Key/Secret HMAC authentication and webhook receiver.

---

## 3. Data & Storage Architecture

| Component | Technology | Primary Responsibility |
|---|---|---|
| Primary Database | PostgreSQL 16 | Relational entities, multi-tenant tables, orders, products, conversations, audit logs |
| Vector Engine | pgvector (Postgres) | High-dimensional embedding storage (1536-d / 768-d) and HNSW cosine similarity index |
| Cache & Message Broker | Redis 7 Alpine | Session state, distributed rate limiting, LLM response caching, fast key-value lookups |
| File & Artifact Storage | Local / S3-compatible | Multimodal image uploads, knowledge attachments |

---

## 4. Security & Tenant Isolation

1. **Multi-Tenancy**: Every database table includes a `tenant_id` column. Tenant context is populated during authentication and enforced in JPA repository specifications.
2. **Zero Arbitrary Execution**: Neither arbitrary SQL nor shell commands can ever be generated or executed by the AI model.
3. **Secret Isolation**: Store access tokens and API keys are stored encrypted and are never exposed to LLM prompts or frontend responses.
4. **Audit Trail**: Every AI tool invocation, action approval, and configuration modification is recorded with timestamp, operator ID, and execution payload.