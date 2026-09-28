# Contributing to Ecommerce AI Staff

Thank you for your interest in contributing to **Ecommerce AI Staff**!

## Development Setup

Please review [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) for complete local development and environment setup instructions.

## How to Contribute

### Reporting Bugs

1. Check existing GitHub Issues first to ensure the bug hasn't already been reported.
2. Open a new issue with a concise, descriptive title.
3. Include clear steps to reproduce, expected behavior, actual logs or stack traces, and screenshots if applicable.
4. Include your environment details: OS, Docker version, Java JDK version, and Node.js version.

### Feature Requests & Enhancements

1. Review the [Roadmap](docs/ROADMAP.md) before proposing major new features.
2. Open an issue to discuss design and architectural fit before submitting extensive PRs.
3. Ensure proposed features adhere to the system design documented in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) and [docs/AGENT-ARCHITECTURE.md](docs/AGENT-ARCHITECTURE.md).

### Pull Requests

1. Fork the repository on GitHub.
2. Create a clean feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. Follow the coding conventions described in [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md).
4. Write comprehensive automated unit and integration tests for all new business logic.
5. Ensure backend tests pass:
   ```bash
   cd backend && mvn test
   ```
6. Ensure the frontend builds cleanly with zero TypeScript errors:
   ```bash
   cd frontend && npm run build
   ```
7. Submit a pull request with a detailed summary of changes and verification steps.

---

## Architecture & Engineering Guidelines

Please review [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) and [docs/SECURITY.md](docs/SECURITY.md). The following architectural rules are strictly enforced in code reviews:

- **Clean Layering**: Do not put business logic directly in REST controllers. Mediate all business logic through services.
- **Store API Abstraction**: Never invoke external commerce APIs (Shopify Admin GraphQL or WooCommerce REST) directly from controllers. All calls must route through the respective integration service clients.
- **LLM Abstraction**: Never invoke LLM providers directly from controllers or frontend components. All AI workflows must execute through the `AgentRuntimeService` and `ToolRegistry`.
- **Tenant Isolation**: Maintain strict tenant boundaries across every repository query and service context.
- **Database Migrations**: All schema modifications must be versioned via Flyway SQL migration scripts under `backend/src/main/resources/db/migration/`.
- **Security & Safety**:
  - Never generate or execute arbitrary SQL or shell scripts from AI models.
  - High-impact or destructive actions (refunds, cancellations, inventory changes) must be routed through the Human-in-the-Loop `ActionApproval` workflow.
  - Never commit credentials, API keys, or access tokens to source control.

---

## Code Style Standards

- **Java (Backend)**: Follow standard Java conventions. Prefer constructor injection (Lombok `@RequiredArgsConstructor` or explicit constructors). Use Java records for immutable DTOs and API requests/responses.
- **TypeScript (Frontend)**: Follow standard TypeScript best practices. Avoid the `any` type; maintain strong typing for all API models and component props.
- **SQL & Migrations**: Use clean formatting and lowercase SQL keywords for Flyway migration scripts.

---

## Commit Messages

We follow standard [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add WooCommerce webhook verification handler
fix: correct tenant isolation check in order repository
docs: update API documentation for tool registry
test: add unit tests for agent prompt builder
refactor: streamline multimodal speech configuration
```

---

## License

By contributing, you agree that your contributions will be licensed under the [Apache 2.0 License](LICENSE).

