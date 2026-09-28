# Changelog

All notable changes to ShopAI will be documented in this file.
This project adheres to [Semantic Versioning](https://semver.org/).

## [Unreleased]

## [1.0.0-SNAPSHOT] — Phase 1: Infrastructure

### Added
- Repository structure
- AGENTS.md with full rule set
- AI_CONTEXT.md with project summary
- Architecture documentation (ARCHITECTURE.md, DATABASE.md, API.md, SECURITY.md, ROADMAP.md, DEVELOPMENT.md, DEPLOYMENT.md, RAG.md, GRAPH-RAG.md, AGENT-ARCHITECTURE.md)
- Spring Boot 3.x backend (Java 21, Maven)
- React 18 + TypeScript + MUI frontend
- PostgreSQL 16 + pgvector database with performance-optimized schema
- Redis 7 cache
- Nginx reverse proxy
- Docker Compose with health checks
- Flyway migration V1 — initial schema
- Health endpoint: GET /api/v1/health
- OpenAPI/Swagger documentation
- Testcontainers integration tests
- GitHub Actions CI workflow
