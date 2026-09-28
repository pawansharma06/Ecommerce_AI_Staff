# ShopAI API Reference

## Versioning

All APIs are versioned under `/api/v1/`.
Breaking changes require a new version prefix.

## Base URL

```
http://localhost/api/v1
```

## Authentication

Phase 2. JWT Bearer token in Authorization header:
```
Authorization: Bearer <token>
```

## Endpoints

### Health
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | /api/v1/health | None | System health check |

### Auth (Phase 2)
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | /api/v1/auth/login | None | Login |
| POST | /api/v1/auth/refresh | None | Refresh token |
| POST | /api/v1/auth/logout | JWT | Logout |

### Tenants (Phase 2)
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | /api/v1/tenants | ADMIN | List tenants |
| POST | /api/v1/tenants | ADMIN | Create tenant |
| GET | /api/v1/tenants/{id} | ADMIN | Get tenant |

### Agents (Phase 8+)
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | /api/v1/agents | JWT | List agents |
| POST | /api/v1/agents | JWT | Create agent |
| GET | /api/v1/agents/{id} | JWT | Get agent |
| POST | /api/v1/agents/{id}/chat | JWT | Chat with agent |

### Conversations (Phase 9+)
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | /api/v1/conversations | JWT | List conversations |
| GET | /api/v1/conversations/{id} | JWT | Get conversation |
| GET | /api/v1/conversations/{id}/messages | JWT | Get messages |

### Products (Phase 4+)
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | /api/v1/products | JWT | List products |
| GET | /api/v1/products/{id} | JWT | Get product |
| POST | /api/v1/products/search | JWT | Search products |

### Orders (Phase 4+)
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | /api/v1/orders | JWT | List orders |
| GET | /api/v1/orders/{id} | JWT | Get order |

### Knowledge (Phase 6+)
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | /api/v1/knowledge/sources | JWT | List sources |
| POST | /api/v1/knowledge/sources | JWT | Add source |
| DELETE | /api/v1/knowledge/sources/{id} | JWT | Remove source |

### Graph (Phase 5+)
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | /api/v1/graph/customers/{id} | JWT | Customer graph |
| GET | /api/v1/graph/products/{id}/related | JWT | Related products |

### Audit (Phase 2+)
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | /api/v1/audit | JWT | Audit log |

## Standard Response Format

### Success
```json
{
  "success": true,
  "data": { ... },
  "timestamp": "2024-01-01T00:00:00Z",
  "requestId": "uuid"
}
```

### Error
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": [ ... ]
  },
  "timestamp": "2024-01-01T00:00:00Z",
  "requestId": "uuid"
}
```

## OpenAPI

Swagger UI: http://localhost/swagger-ui.html
OpenAPI JSON: http://localhost/api-docs
