# Security Policy

## Supported Versions

| Version | Supported |
|---|---|
| 1.x (current) | ✅ |

## Reporting a Vulnerability

**Do not open a public GitHub issue for security vulnerabilities.**

Please report security vulnerabilities by emailing: security@shopai.dev

Include:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if any)

You will receive a response within 48 hours. We will work with you to understand and resolve the issue before any public disclosure.

## Security Architecture

See [docs/SECURITY.md](docs/SECURITY.md) for the full security architecture.

## Key Security Properties

- Multi-tenant isolation: tenant data is never shared across tenants.
- Credentials are encrypted at rest and never appear in logs or LLM context.
- All AI actions are audited.
- Dangerous actions require human approval.
- Webhook HMAC verification required.
- RBAC with fine-grained permissions.
