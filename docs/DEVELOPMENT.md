# ShopAI Development Guide

## Prerequisites

- Docker Desktop 4.x+
- Java 21 JDK (for IDE development without Docker)
- Node.js 20+ (for frontend development without Docker)
- Git

## Quick Start

```bash
git clone https://github.com/your-org/shopai.git
cd shopai
cp .env.example .env
# Edit .env if needed (defaults work for local dev)
docker compose up -d
```

## Access Points

| Service | URL |
|---|---|
| Frontend | http://localhost |
| API | http://localhost/api/v1 |
| Swagger UI | http://localhost/swagger-ui.html |
| Health | http://localhost/api/v1/health |
| PostgreSQL | localhost:5432 |
| Redis | localhost:6379 |

## Backend Development

### Run without Docker

```bash
# Start only infrastructure
docker compose up -d postgres redis

# Run backend
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Build

```bash
cd backend
./mvnw clean install
```

### Test

```bash
cd backend
./mvnw verify
```

Tests use Testcontainers — Docker must be running.

### Package Structure

```
com.shopai
├── common/
│   ├── config/        # Spring configuration classes
│   ├── dto/           # Shared DTOs (records)
│   ├── exception/     # Exception types + GlobalExceptionHandler
│   └── health/        # Health controller and service
├── auth/              # Phase 2
├── tenant/            # Phase 2
└── ...                # Future phases
```

## Frontend Development

### Run without Docker

```bash
# Backend must be running (see above)
cd frontend
npm install
npm run dev
```

Frontend runs at http://localhost:5173 with proxy to backend.

### Build

```bash
cd frontend
npm run build
```

## Coding Conventions

### Java
- Constructor injection only (no @Autowired field injection).
- Use records for DTOs.
- Use typed exceptions.
- No business logic in controllers.
- No direct DB access in services — use repositories.
- All external integrations behind interfaces.
- Structured logging with SLF4J.
- Never log credentials or sensitive data.

### TypeScript
- No `any` types.
- Use TanStack Query for server state.
- No direct fetch calls in components — use API hooks.
- MUI components only for UI.

### SQL / Flyway
- All schema changes via Flyway migration files.
- Never modify existing migration files.
- Version format: `V{N}__{description}.sql`.
- Document complex queries with comments.

## Adding a New Module

1. Create package under `com.shopai.{module}`.
2. Create subpackages: controller, service, repository, domain, dto, exception.
3. Register any new beans via @Component, @Service, @Repository.
4. Add new API paths to docs/API.md.
5. Add new tables via Flyway migration.
6. Write tests before marking complete.
