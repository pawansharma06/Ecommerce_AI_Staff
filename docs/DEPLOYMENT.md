# ShopAI Deployment Guide

## Local Development

See [DEVELOPMENT.md](DEVELOPMENT.md).

## Docker Compose Production Deployment

### Minimal Requirements

- Ubuntu 22.04+ or equivalent Linux server
- Docker 24+
- Docker Compose v2
- 4GB RAM minimum (8GB recommended)
- 20GB disk minimum

### Steps

```bash
git clone https://github.com/your-org/shopai.git
cd shopai
cp .env.example .env

# Configure production values in .env:
# - POSTGRES_PASSWORD (strong password)
# - JWT_SECRET (32+ random chars)
# - All API keys

docker compose up -d
```

### Production Checklist

- [ ] Change all default passwords
- [ ] Set JWT_SECRET to 32+ random characters
- [ ] Configure Nginx TLS (HTTPS) with certbot
- [ ] Remove exposed PostgreSQL/Redis ports from docker-compose.yml
- [ ] Configure server firewall (allow 80, 443 only)
- [ ] Set up automated database backups
- [ ] Monitor with: `docker compose logs -f`

## Environment Variables

See `.env.example` for the full list.

## Health Check

```bash
curl https://your-domain/api/v1/health
```

## Backup

```bash
# PostgreSQL backup
docker compose exec postgres pg_dump -U shopai shopai > backup_$(date +%Y%m%d).sql

# Restore
cat backup_20240101.sql | docker compose exec -T postgres psql -U shopai shopai
```

## Scaling

For high traffic, consider:
1. Scale backend replicas (update docker-compose.yml).
2. Add PgBouncer connection pooler in front of PostgreSQL.
3. Use Redis Sentinel or Redis Cluster for HA.
4. Move to managed PostgreSQL (RDS, Cloud SQL, Supabase).

## Future Kubernetes Deployment

The Docker Compose setup maps directly to Kubernetes:
- Each service → Deployment
- PostgreSQL → StatefulSet or managed DB
- Redis → StatefulSet or managed cache
- Nginx → Ingress controller
- .env → ConfigMaps + Secrets
