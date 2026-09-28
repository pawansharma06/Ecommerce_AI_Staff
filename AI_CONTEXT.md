# ShopAI AI Context

## Project

ShopAI — Open-Source Shopify-Centric AI Agent Platform

## Purpose

A self-hosted, multi-tenant, open-source AI Agent Platform designed for Shopify merchants.
Provides AI-powered customer support, Shopify administration, product intelligence,
order assistance, inventory intelligence, reporting, knowledge retrieval,
Graph RAG, multi-LLM support, WhatsApp, email, and an extensible tool/agent framework.
Shopify is the first commerce platform. The architecture supports adding WooCommerce,
Magento, BigCommerce, and custom commerce APIs in the future.

## Backend

- Java 21 LTS
- Spring Boot 3.x
- Maven
- Modular monolith — microservice-ready

## Database

- PostgreSQL 16 + pgvector
- Optimized for large datasets:
  - HNSW indexes on vector columns
  - B-tree indexes on all FK and query columns
  - Composite indexes for multi-column patterns
  - GIN indexes on JSONB metadata columns
  - BRIN indexes on append-only timestamp columns
  - Range partitioning on high-volume tables (audit_logs, conversation_messages, webhook_events, llm_usage)
  - Tuned postgresql.conf: shared_buffers, work_mem, parallel workers
  - HikariCP connection pool: pool size 20, leak detection enabled
- 3NF normalization throughout

## Cache

- Redis 7
- Used for: cache, queues, rate limiting, locks, conversation state, webhook processing

## Frontend

- React 18 + TypeScript + MUI
- Vite build
- TanStack Query for server state
- React Router for navigation

## Infrastructure

- Docker + Docker Compose
- Nginx reverse proxy

## AI

- Multiple LLM providers (all behind LLMProvider interface):
  - OpenAI
  - Anthropic Claude
  - Google Gemini
  - xAI Grok
  - OpenAI-compatible custom APIs
  - Future: Ollama, vLLM

## Retrieval Architecture

- Vector RAG = unstructured knowledge (FAQ, policies, documents) via pgvector
- Graph RAG = entity relationships (customer→order→product) via PostgreSQL graph tables
- Shopify Tools = live commerce state and actions via Shopify GraphQL API
- Hybrid = ContextEngine combines all three

## Multi-Tenancy

- Hierarchy: Tenant → Shops → Users → Agents → Conversations → Knowledge
- Every tenant-owned record has tenant_id
- Tenant resolved from security context, never from client input
- Repository-level tenant isolation enforced

## Security

- Spring Security
- RBAC with fine-grained permissions
- Credentials encrypted at rest, never in LLM context
- Full audit logging
- Webhook HMAC verification

## Modules

auth, tenant, shopify, customer, product, order, inventory,
agent, tool, llm, rag, graph, knowledge, conversation,
analytics, reporting, whatsapp, email, audit

## Current Phase

PHASE 1 — Infrastructure only.
Do NOT implement future phases unless explicitly requested.