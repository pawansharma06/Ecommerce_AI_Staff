# ShopAI Security Architecture

## Authentication

- Spring Security
- JWT tokens (stateless)
- Token expiry configurable
- Refresh token support (Phase 2)

## Authorization

- RBAC: Role-Based Access Control
- Fine-grained permissions:
  - product.read, product.create, product.update, product.delete
  - order.read, order.update, order.cancel
  - inventory.read, inventory.update
  - customer.read
  - report.read, report.execute
  - agent.execute
  - integration.manage
  - knowledge.manage
  - audit.read

## Tenant Isolation

- Every tenant-owned record has `tenant_id`.
- Tenant ID is resolved from the authenticated security context — never from client input.
- Repository layer enforces tenant scoping in every query.
- Automated tests prove Tenant A cannot access Tenant B data.

## Credential Security

- Shopify access tokens encrypted at rest (AES-256).
- LLM API keys stored encrypted.
- Credentials never appear in:
  - Frontend responses
  - Application logs
  - LLM prompts
  - Audit log payloads

## Webhook Security

- HMAC-SHA256 verification for all Shopify webhooks.
- Idempotency checks prevent duplicate processing.
- Webhook payloads stored for audit/replay.

## Rate Limiting

- Redis-backed rate limiting per tenant and per user.
- Configurable limits per endpoint.

## Input Validation

- Bean Validation on all DTOs.
- SQL injection prevented via JPA parameterized queries.
- No arbitrary SQL from LLM.
- No arbitrary code execution.

## Audit Logging

- All AI actions recorded in `audit_logs`.
- All admin actions recorded.
- All security events recorded.
- Audit log is append-only and partitioned for performance.
- Audit records never contain credentials.

## Production Security Checklist

- [ ] Change all default passwords in .env
- [ ] Set strong JWT_SECRET (32+ chars)
- [ ] Enable HTTPS via Nginx TLS
- [ ] Configure firewall: expose only port 80/443
- [ ] Do not expose PostgreSQL or Redis ports in production
- [ ] Rotate credentials regularly
- [ ] Monitor audit_logs for anomalies
