# 🛍️ Ecommerce AI Staff (ShopAI)

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.x-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.x-61dafb.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.x-blue.svg)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16%20+%20pgvector-336791.svg)](https://github.com/pgvector/pgvector)
[![Redis](https://img.shields.io/badge/Redis-7-red.svg)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ed.svg)](https://www.docker.com/)

**Ecommerce AI Staff** is a production-grade, self-hosted, multi-tenant AI workforce and orchestration platform for modern e-commerce stores. It acts as an autonomous virtual operations team capable of handling 24/7 customer support, live catalog search, order management, inventory tracking, policy Q&A, multimodal interactions (Voice & Vision), and proactive business analytics.

---

## 📑 Table of Contents

- [Overview & Architecture](#-overview--architecture)
- [Core Features](#-core-features)
- [Technology Stack](#-technology-stack)
- [Repository Structure](#-repository-structure)
- [Prerequisites](#-prerequisites)
- [Step-by-Step Setup Guide](#-step-by-step-setup-guide)
  - [Method 1: Docker Compose (Quickstart)](#method-1-docker-compose-quickstart)
  - [Method 2: Local Development Setup](#method-2-local-development-setup)
- [Configuration & Initial Setup](#-configuration--initial-setup)
  - [1. Admin Login](#1-admin-login)
  - [2. Commerce Store Integration (Shopify & WooCommerce)](#2-commerce-store-integration-shopify--woocommerce)
  - [3. AI Provider Configuration (OpenAI, Gemini, Local LLM)](#3-ai-provider-configuration-openai-gemini-local-llm)
  - [4. Multimodal Setup (TTS, STT, Vision)](#4-multimodal-setup-tts-stt-vision)
- [Human-in-the-Loop & Safety Guardrails](#-human-in-the-loop--safety-guardrails)
- [API & Swagger Documentation](#-api--swagger-documentation)
- [Database & Migrations](#-database--migrations)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🌟 Overview & Architecture

Ecommerce AI Staff bridges the gap between Large Language Models (LLMs) and real-time commerce data. Instead of generic chatbot responses, it executes real business actions through structured tool calling, deterministic safety gates, hybrid vector search (pgvector HNSW), and knowledge graph entity mapping.

```
                      ┌──────────────────────────────────────────────────────────┐
                      │              Customer Channels                           │
                      │       (Web Widget / WhatsApp / Email)                   │
                      └────────────────────────┬─────────────────────────────────┘
                                               │
                                               ▼
                      ┌──────────────────────────────────────────────────────────┐
                      │                 Nginx Reverse Proxy                      │
                      └────────────────────────┬─────────────────────────────────┘
                                               │
                         ┌─────────────────────┴─────────────────────┐
                         ▼                                           ▼
          ┌─────────────────────────────┐             ┌─────────────────────────────┐
          │     React 18 Frontend       │             │   Spring Boot 3.3 Backend   │
          │ (Vite + TypeScript + MUI)   │             │   (Java 21 LTS + Security)  │
          └─────────────────────────────┘             └──────────────┬──────────────┘
                                                                     │
              ┌──────────────────────────────────────────────────────┼──────────────────────────────────────────────────────┐
              ▼                                                      ▼                                                      ▼
┌───────────────────────────┐                          ┌───────────────────────────┐                          ┌───────────────────────────┐
│     AI Agent Engine       │                          │     Hybrid RAG Engine     │                          │     Commerce Connectors   │
│  - Tool Registry          │                          │  - pgvector HNSW Vector   │                          │  - Shopify GraphQL & Hooks│
│  - Human Approval Gates   │                          │  - Entity Knowledge Graph │                          │  - WooCommerce REST v3    │
│  - Multi-LLM Orchestrator │                          │  - Policy & FAQ Documents │                          │  - Real-time Webhooks     │
└─────────────┬─────────────┘                          └─────────────┬─────────────┘                          └─────────────┬─────────────┘
              │                                                      │                                                      │
              └──────────────────────────────────────────────────────┼──────────────────────────────────────────────────────┘
                                                                     │
                                       ┌─────────────────────────────┴─────────────────────────────┐
                                       ▼                                                           ▼
                        ┌─────────────────────────────┐                             ┌─────────────────────────────┐
                        │   PostgreSQL 16 + pgvector  │                             │           Redis 7           │
                        │ (Orders, Products, Vectors) │                             │  (Cache, Queues, Sessions)  │
                        └─────────────────────────────┘                             └─────────────────────────────┘
```

---

## ✨ Core Features

| Feature | Description |
|---|---|
| 🤖 **Autonomous AI Staff Engine** | Multi-turn reasoning loop with deterministic tool calling and structured output parsing. |
| 🛍️ **Multi-Store Private Apps** | Dedicated integration management for **Shopify** (Admin GraphQL API) and **WooCommerce** (REST API v3). |
| 🧠 **Multi-LLM Orchestration** | Choose your primary provider: **OpenAI (GPT-4o, GPT-4o-mini)**, **Google Gemini (1.5 Pro, 1.5 Flash)**, or **Local Fallback (Ollama / Self-hosted)**. |
| 🎙️ **Multimodal Voice & Vision** | Built-in Text-to-Speech (TTS), Speech-to-Text (STT Whisper), and Vision AI for visual product search and defect inspection. |
| 🛡️ **Human-in-the-Loop Approval** | High-impact actions (refunds, order cancellation, discount codes, inventory write) trigger human authorization gates before execution. |
| 🔍 **Hybrid Vector + Graph RAG** | Combines pgvector cosine similarity search (`HNSW` index) with relational Knowledge Graph discovery (Customer-Product-Order associations). |
| 💬 **Omni-Channel Customer Messaging** | Connect customers seamlessly through Webchat, WhatsApp Cloud API, and Email (SMTP/IMAP). |
| 🏢 **Enterprise Multi-Tenancy** | Strict database-level and query-level tenant isolation with granular Role-Based Access Control (RBAC). |
| 📊 **Real-Time Analytics & Audit** | Complete observability with tool call counts, token consumption tracking, response latency, and immutable audit logs. |

---

## 🛠️ Technology Stack

### Backend
- **Language**: Java 21 LTS
- **Framework**: Spring Boot 3.3.x (Spring Web, Spring Security, Spring Data JPA, Spring Validation)
- **Database Access**: Hibernate 6.x + PostgreSQL Dialect
- **Vector Search**: pgvector (`vector` type with HNSW index)
- **Migrations**: Flyway
- **Security**: JWT Authentication + BCrypt Password Encoding + RBAC
- **Build Tool**: Apache Maven 3.9+

### Frontend
- **Framework**: React 18
- **Language**: TypeScript 5.x
- **Build Tool**: Vite
- **UI Components**: Material UI (MUI v5) + Emotion + Lucide Icons
- **State & Data Fetching**: TanStack React Query v5 + React Router v6

### Storage & Caching
- **Primary Database**: PostgreSQL 16
- **Vector Extension**: `pgvector`
- **Cache & Session Queue**: Redis 7 Alpine

### Infrastructure
- **Containerization**: Docker & Docker Compose
- **Reverse Proxy**: Nginx Alpine

---

## 📂 Repository Structure

```
Ecommerce_AI_Staff/
├── backend/                        # Java 21 Spring Boot Application
│   ├── src/main/java/com/shopai/
│   │   ├── agent/                  # Agent loop, Planner & Execution Engine
│   │   ├── analytics/              # Usage, Telemetry & Analytics Services
│   │   ├── auth/                   # JWT, User Details, Authentication Controllers
│   │   ├── channels/               # WhatsApp, Email, Webchat handlers
│   │   ├── common/                 # Base entities, Exceptions, Tenant Context
│   │   ├── config/                 # Security, Redis, Async, OpenAPI configs
│   │   ├── customer/               # Customer entities and repositories
│   │   ├── graph/                  # Graph RAG & Commerce relationship mapping
│   │   ├── knowledge/              # Vector RAG & Document embeddings
│   │   ├── llm/                    # OpenAI, Gemini, Local Fallback providers
│   │   ├── order/                  # Order management and tracking
│   │   ├── product/                # Product catalog and sync
│   │   ├── shopify/                # Shopify Admin GraphQL client & Webhooks
│   │   ├── tools/                  # Tool Registry (Search, Order, Refund, etc.)
│   │   └── woocommerce/            # WooCommerce REST client & Webhooks
│   ├── src/main/resources/
│   │   ├── db/migration/           # Flyway SQL schema migrations
│   │   └── application.yml         # Spring configuration profiles
│   └── pom.xml                     # Maven dependencies
├── frontend/                       # React 18 TypeScript Vite Application
│   ├── src/
│   │   ├── components/             # Reusable UI components & layouts
│   │   ├── pages/                  # Dashboard, Integrations, Settings, Analytics, etc.
│   │   ├── services/               # API clients & Axios interceptors
│   │   ├── types/                  # TypeScript interface definitions
│   │   └── App.tsx                 # Main routing and theme setup
│   ├── package.json                # NPM dependencies
│   └── vite.config.ts              # Vite bundler configuration
├── docker/                         # Docker & Nginx configurations
│   ├── nginx/nginx.conf            # Reverse proxy configuration
│   └── postgres/postgresql.conf    # PostgreSQL performance tuning
├── docs/                           # Detailed Architecture & Technical Docs
│   ├── ARCHITECTURE.md             # In-depth architectural blueprint
│   ├── AGENT-ARCHITECTURE.md       # AI Agent & Tool Registry specification
│   ├── DATABASE.md                 # Database schema & indexing guide
│   ├── RAG.md                      # Vector RAG implementation
│   ├── GRAPH-RAG.md                # Knowledge Graph implementation
│   ├── API.md                      # REST API endpoint reference
│   ├── SECURITY.md                 # Security & isolation model
│   └── DEPLOYMENT.md               # Production deployment runbook
├── docker-compose.yml              # Complete containerized stack
├── Dockerfile.backend              # Multi-stage Java build container
├── Dockerfile.frontend             # Multi-stage Node/Vite build container
└── README.md                       # Project documentation
```

---

## 📋 Prerequisites

Before starting, ensure you have the following installed on your machine:

- **Git**: `git --version` (v2.30+)
- **Docker & Docker Compose**: `docker compose version` (v2.20+)
- *(Optional for direct local development)*:
  - **JDK 21**: OpenJDK 21 or Eclipse Temurin 21
  - **Maven 3.9+**: `mvn -v`
  - **Node.js 20+ & npm**: `node -v` && `npm -v`

---

## 🚀 Step-by-Step Setup Guide

### Method 1: Docker Compose (Quickstart)

This is the fastest and recommended way to bring up the entire stack (Postgres + pgvector, Redis, Backend, Frontend, and Nginx).

#### 1. Clone the Repository
```bash
git clone https://github.com/pawansharma06/Ecommerce_AI_Staff.git
cd Ecommerce_AI_Staff
```

#### 2. Create Environment Configuration
```bash
cp .env.example .env
```
*(Optionally review `.env` and adjust database passwords or ports).*

#### 3. Build and Start All Services
```bash
docker compose up -d --build
```

#### 4. Verify Service Health
```bash
docker compose ps
```
All 5 containers (`shopai-nginx`, `shopai-backend`, `shopai-frontend`, `shopai-postgres`, `shopai-redis`) should report `Up` / `healthy`.

---

### Method 2: Local Development Setup

If you wish to run the backend and frontend directly on your host machine for development:

#### 1. Start Infrastructure Containers (Postgres + Redis)
```bash
docker compose up -d postgres redis
```

#### 2. Configure Backend Environment
Set your environment variables or check `backend/src/main/resources/application.yml`.
Key default properties:
- `POSTGRES_HOST=localhost`
- `POSTGRES_PORT=5432`
- `POSTGRES_DB=shopai`
- `POSTGRES_USER=shopai`
- `POSTGRES_PASSWORD=shopai`
- `REDIS_HOST=localhost`
- `REDIS_PORT=6379`

#### 3. Run Backend (Spring Boot)
```bash
cd backend
mvn clean compile
mvn spring-boot:run
```
*Note: The backend runs on `http://localhost:8080` (or `http://localhost:8085` depending on port config).*

#### 4. Run Frontend (React Vite)
Open a new terminal window:
```bash
cd frontend
npm install
npm run dev
```
*The frontend Vite dev server will start at `http://localhost:3000`.*

---

## ⚙️ Configuration & Initial Setup

### 1. Admin Login
Access the web dashboard at **`http://localhost`** (or `http://localhost:3000`).

Default Seed Admin Credentials:
- **Email**: `admin@shopai.dev`
- **Password**: `AdminPassword123!`

---

### 2. Commerce Store Integration (Shopify & WooCommerce)

Navigate to **Integrations** (`/integrations`) in the sidebar:

#### Setting up Shopify:
1. In your Shopify Admin, create a **Custom App** under **Settings > Apps and sales channels > Develop apps**.
2. Grant Admin API scopes (`read_products`, `read_orders`, `read_customers`, `read_inventory`).
3. Install the app and copy the **Admin API Access Token** (`shpat_...`).
4. In Ecommerce AI Staff, enter:
   - **Store Domain**: `your-store.myshopify.com`
   - **Access Token**: `shpat_xxxxxxxxxxxxxxxxxxxxxxxx`
   - **Webhook Secret**: Your app secret
5. Click **Test Connection** & **Save Configuration**.

#### Setting up WooCommerce:
1. In your WordPress Admin, go to **WooCommerce > Settings > Advanced > REST API**.
2. Click **Add key**, name it `Ecommerce AI Staff`, and set permissions to **Read/Write**.
3. In Ecommerce AI Staff, enter:
   - **Store URL**: `https://your-store.com`
   - **Consumer Key**: `ck_xxxxxxxxxxxxxxxxxxxxxxxx`
   - **Consumer Secret**: `cs_xxxxxxxxxxxxxxxxxxxxxxxx`
4. Click **Test Connection** & **Save Configuration**.

---

### 3. AI Provider Configuration (OpenAI, Gemini, Local LLM)

Navigate to **Settings** (`/settings`) in the sidebar:

1. **Select Primary Model**:
   - `OpenAI (ChatGPT)`
   - `Google Gemini`
   - `Local Fallback (Ollama / Offline Mock)`
2. **Configure OpenAI / ChatGPT**:
   - Enter your `OpenAI API Key` (`sk-...`).
   - Select model (e.g. `gpt-4o`, `gpt-4o-mini`, `gpt-4-turbo`).
   - Click **Validate API Key**.
3. **Configure Google Gemini**:
   - Enter your `Gemini API Key` (`AIza...`).
   - Select model (e.g. `gemini-1.5-pro`, `gemini-1.5-flash`).
   - Click **Validate API Key**.

---

### 4. Multimodal Setup (TTS, STT, Vision)

On the **Settings** (`/settings`) page under **Multimodal Settings**:
- **Text-to-Speech (TTS)**: Select preferred speech synthesis voice (`alloy`, `echo`, `nova`, etc.) and speech rate (`0.5x` - `2.0x`).
- **Speech-to-Text (STT)**: Configure OpenAI Whisper model (`whisper-1`) and primary transcription language.
- **Vision AI**: Select image detail resolution (`auto`, `low`, `high`) for visual product search and photo damage inspection.

---

## 🔒 Human-in-the-Loop & Safety Guardrails

Ecommerce AI Staff implements strict enterprise guardrails:
1. **Zero Arbitrary Execution**: The LLM cannot execute raw SQL or arbitrary shell code. All operations are mediated through typed Java tools in the `ToolRegistry`.
2. **Approval-Gated Operations**: Any mutating action (e.g., executing a refund, canceling an order, or adjusting inventory) automatically generates an `ActionRequest` with `PENDING` status.
3. **Operator Notification**: Operators can approve or reject the action directly in the **Action Approvals** dashboard before any real API call touches Shopify or WooCommerce.
4. **Credential Isolation**: API keys and store access tokens are stored securely in PostgreSQL and are **never** injected into LLM context prompts.

---

## 📖 API & Swagger Documentation

When the backend is running, you can explore the interactive OpenAPI / Swagger documentation:

- **Swagger UI**: `http://localhost/swagger-ui.html` (or `http://localhost:8080/swagger-ui.html`)
- **OpenAPI JSON**: `http://localhost/api/v1/api-docs`

Key API Endpoints:
- `POST /api/v1/auth/login` — User authentication & JWT generation
- `POST /api/v1/agent/chat` — Core AI conversation & reasoning endpoint
- `GET  /api/v1/agent/tools` — List registered AI tools and execution schema
- `GET  /api/v1/actions/pending` — List pending human-in-the-loop action requests
- `POST /api/v1/actions/{id}/approve` — Approve a pending AI action
- `POST /api/v1/shopify/config` — Update Shopify store credentials
- `POST /api/v1/woocommerce/config` — Update WooCommerce credentials
- `GET  /api/v1/analytics/dashboard` — Live usage, token, and tool metrics
- `GET  /api/v1/health` — Application, Database, and Cache health check

---

## 🗄️ Database & Migrations

Database schema versioning is managed automatically via **Flyway**:
- Migration files are located in: [`backend/src/main/resources/db/migration/`](file:///c:/apps/ShopAI/backend/src/main/resources/db/migration/)
- `V1__init_schema.sql` — Multi-tenant tables, users, products, orders, conversations, action requests, vector embeddings.
- `V2__graph_rag.sql` — Commerce relationship nodes and edges for Graph RAG.
- `V3__knowledge_base.sql` — Knowledge base collections and vector embeddings.
- `V4__analytics_and_audit.sql` — Usage logging and security audit trail.

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:
1. Fork the repository.
2. Create your feature branch (`git checkout -b feature/amazing-feature`).
3. Commit your changes (`git commit -m 'Add amazing feature'`).
4. Push to the branch (`git push origin feature/amazing-feature`).
5. Open a Pull Request.

---

## 📄 License

Distributed under the Apache 2.0 License. See [`LICENSE`](LICENSE) for more information.

