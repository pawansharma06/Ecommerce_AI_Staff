# Contributing to ShopAI

Thank you for your interest in contributing to ShopAI!

## Development Setup

See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) for complete setup instructions.

## How to Contribute

### Reporting Bugs

1. Check existing issues first.
2. Create a new issue with a clear title and description.
3. Include steps to reproduce, expected behavior, and actual behavior.
4. Include your environment: OS, Docker version, Java version.

### Feature Requests

1. Check the [ROADMAP.md](docs/ROADMAP.md) first.
2. Open an issue to discuss before implementing.
3. Features must align with the architecture documented in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

### Pull Requests

1. Fork the repository.
2. Create a feature branch: `git checkout -b feature/your-feature`.
3. Follow the coding conventions in [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md).
4. Write tests for all business logic.
5. Ensure all tests pass: `cd backend && ./mvnw verify`.
6. Ensure the frontend builds: `cd frontend && npm run build`.
7. Submit a pull request with a clear description.

## Architecture Rules

Before contributing, read [AGENTS.md](AGENTS.md). These rules are enforced in code review:

- No business logic in controllers.
- No direct Shopify API calls from controllers.
- No direct LLM calls from controllers.
- All tenant data must be tenant-isolated.
- All schema changes must use Flyway.
- Tests required for business logic.
- No arbitrary SQL from LLM.
- No credentials in source code.

## Code Style

- Java: Follow standard Java conventions. Use constructor injection. Prefer records for DTOs.
- TypeScript: Follow standard TypeScript conventions. No `any` types.
- SQL: Use lowercase keywords. Document complex queries.

## Commit Messages

Use conventional commits:

```
feat: add product search endpoint
fix: correct tenant isolation in order repository
docs: update API documentation
test: add integration tests for webhook HMAC
refactor: extract ShopifyClient interface
```

## License

By contributing, you agree that your contributions will be licensed under the Apache 2.0 License.
