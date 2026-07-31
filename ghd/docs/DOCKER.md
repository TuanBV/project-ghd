# Docker operations guide

This document covers building, running, and operating the GHD stack (app + MySQL +
Redis) with Docker Compose.

## Prerequisites

- Docker Engine 24+ and Docker Compose v2 (the `docker compose` subcommand, not the
  old standalone `docker-compose`)
- Node.js is **not** required on the host - the frontend is built inside the Docker
  image. It's only needed for [local development outside Docker](#running-locally-without-docker).

## 1. Configure environment

```bash
cp .env.example .env
```

Edit `.env` and fill in real values. Required variables (compose will refuse to
start without these - they have no default):

| Variable | Purpose |
|---|---|
| `DB_USERNAME`, `DB_PASSWORD` | App's MySQL user/password (created automatically in the `core` database) |
| `MYSQL_ROOT_PASSWORD` | MySQL root superuser password (container administration only, the app never uses it) |
| `REDIS_PASSWORD` | Redis auth password |
| `JWT_SECRET` | HMAC secret for JWT signing - generate with `openssl rand -hex 64` |
| `AES_SECRET` | Secret for AES-encrypted values - generate with `openssl rand -hex 64` |

Everything else in `.env.example` has a safe default and is optional to change
(mail/Telegram credentials, cookie/session settings, ports).

**Rotate compromised secrets**: `src/main/resources/application.properties` used to
contain real credentials committed to git history (DB password, JWT secret, AES key,
a Gmail app password, and a Telegram bot token). Those are now read from environment
variables, but the old values are still visible in git history and must be treated as
leaked - generate brand new values for all of them (new DB password, new
`JWT_SECRET`/`AES_SECRET`, a new Gmail app password, a new Telegram bot token) rather
than reusing what's in history.

## 2. Build and start

```bash
docker compose build
docker compose up -d
```

`app` waits for `mysql` and `redis` to report healthy before starting
(`depends_on: condition: service_healthy`). On first boot, Flyway runs all
migrations in `src/main/resources/db/migration` against the empty `core` database -
this is the only thing that creates the schema; nothing in this repo mounts or runs
`scripts/mysql-init.sql` against the compose database (that script references a
different database/user - `manageruser`/`admin` - left over from another
environment; don't run it here).

## 3. Logs

```bash
docker compose logs -f app
docker compose logs -f mysql redis
```

Application logs also land on the `logs_data` volume (`LOG_PATH=/app/logs` inside
the container), so they survive container restarts/recreation.

## 4. Health checks

```bash
curl http://localhost:${APP_PORT:-8080}/actuator/health  # e.g. http://localhost:18080/actuator/health
```

Only `health` and `info` are exposed (`management.endpoints.web.exposure.include`),
and health detail (component-level DB/Redis status) only shows for authorized
requests (`management.endpoint.health.show-details=when-authorized`) - anonymous
callers (including the container healthcheck) just get `{"status":"UP"}` or `DOWN`,
never internal connection details.

Compose-level health status:

```bash
docker compose ps
```

## 5. Database migrations

Flyway runs automatically on every app startup - there is no separate migration
command to run. To add a new migration, add a new
`src/main/resources/db/migration/V<next>__description.sql` file and restart the
`app` service.

## 6. Backup / restore the MySQL volume

Backup (logical dump, safe to do live):

```bash
docker compose exec mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction core' > backup-$(date +%Y%m%d-%H%M%S).sql
```

Restore into a running, empty database:

```bash
docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" core' < backup-YYYYMMDD-HHMMSS.sql
```

Full volume backup/restore (stop the app first so nothing is writing):

```bash
docker compose stop app
docker run --rm -v ghd_mysql_data:/data -v "$PWD":/backup alpine \
  tar czf /backup/mysql-data-$(date +%Y%m%d-%H%M%S).tar.gz -C /data .
docker compose start app
```

## 7. Backup uploads / SEO files

```bash
docker run --rm -v ghd_uploads_data:/data -v "$PWD":/backup alpine \
  tar czf /backup/uploads-$(date +%Y%m%d-%H%M%S).tar.gz -C /data .

docker run --rm -v ghd_seo_data:/data -v "$PWD":/backup alpine \
  tar czf /backup/seo-$(date +%Y%m%d-%H%M%S).tar.gz -C /data .
```

## 8. Flushing cache (Redis)

Cache keys are namespaced as `<app>:<environment>::<cacheName>::<key>` (see
`app.cache.namespace` / `CacheConfig`), and sessions as
`<app>:<environment>:session:...` (see `spring.session.redis.namespace`) - **never**
run `FLUSHALL`/`FLUSHDB`, since that would also wipe every active login session.

To clear one cache region (e.g. after a manual DB edit that bypassed the app):

```bash
docker compose exec redis sh -c \
  'redis-cli -a "$REDIS_PASSWORD" --no-auth-warning --scan --pattern "ghd:*::categories::*" | \
   xargs -r redis-cli -a "$REDIS_PASSWORD" --no-auth-warning DEL'
```

Replace `categories` with `brands`, `banners`, `sliders`, `policies`, `sys-params`,
`products`, `products-latest`, `products-related`, or `news` as needed (see
`CacheConfig` for the full list). Normally this isn't necessary - every write path
already evicts the caches it affects.

## 9. Running locally without Docker

The application still works standalone with local defaults (`localhost` MySQL/Redis,
`uploads`/`seo`/`logs` relative directories) - nothing requires Docker:

```bash
npm ci && npm run build:frontend
./mvnw spring-boot:run
```

Override any property via `-D` or environment variables the same way as in `.env`
(e.g. `DB_PASSWORD`, `REDIS_HOST`). If you don't have a local Redis, temporarily set
`spring.cache.type=none` and `spring.session.store-type=none` (or point `REDIS_HOST`
at a Redis you do have) - the app's business logic behaves the same either way, just
without cache/session persistence.

## 10. Publishing MySQL/Redis ports for debugging

By default `mysql`/`redis` are only reachable from other containers on the compose
network - no host ports are published. To connect a DB client or `redis-cli` from
the host temporarily:

```bash
docker compose -f compose.yaml -f compose.debug.yaml up -d
```

This publishes `${DB_DEBUG_PORT:-3306}` and `${REDIS_DEBUG_PORT:-6379}` to the host.
Do not use this override on any shared/production host.

## 11. Using an external MySQL (VPS)

If MySQL already runs on the host (a VPS) instead of in a container, use
`compose.external-db.yaml` instead of `compose.yaml`. It only starts `app` and
`redis` - it is a standalone file, not an override, since Compose overrides can add
fields to a service but cannot remove a service defined in another file (so
`-f compose.yaml -f compose.external-db.yaml` would still start `mysql`).

```bash
cp .env.example .env
# Set DB_HOST to reach MySQL on the host: host.docker.internal (works via the
# extra_hosts entry in compose.external-db.yaml, Docker 20.10+) or the host's
# private/LAN IP. Fill in DB_USERNAME/DB_PASSWORD for that existing MySQL instance.
docker compose -f compose.external-db.yaml up -d --build
```

### Auto-fallback to containerized MySQL

`scripts/deploy.sh` picks the right file for you: it reads `DB_HOST`/`DB_PORT` from
`.env` and probes that host/port. If it's reachable, it runs
`compose.external-db.yaml`; if `DB_HOST` is unset/left at the default `mysql`, or the
probe fails (e.g. no MySQL installed on the VPS yet), it falls back to `compose.yaml`
so the app still comes up with a containerized MySQL instead of failing to start.

```bash
cp .env.example .env
# fill in DB_USERNAME/DB_PASSWORD/etc as usual; set DB_HOST only if you have an
# external MySQL to try first
./scripts/deploy.sh
```

Requirements on the host MySQL:

- Listen on an interface reachable from containers, not only `127.0.0.1`
  (`bind-address=0.0.0.0`, or at least the `docker0` bridge address).
- Grant the app's DB user access from the Docker bridge subnet (typically
  `172.17.0.0/16`), not just `localhost`.
- Allow that subnet through any host firewall on the MySQL port.

Flyway still runs on app startup and owns schema creation - point `DB_NAME` at an
empty database the first time, exactly like the containerized-MySQL setup.

## 12. Rotating secrets

1. Generate new values (`openssl rand -hex 64` for `JWT_SECRET`/`AES_SECRET`, a
   strong random string for `DB_PASSWORD`/`MYSQL_ROOT_PASSWORD`/`REDIS_PASSWORD`).
2. Update `.env`.
3. `docker compose up -d` (recreates `app`/`mysql`/`redis` with the new values;
   MySQL/Redis re-read their root/auth passwords only on container recreation, not
   just restart, so make sure `docker compose up -d` actually recreates them - it
   will, since the environment changed).
4. Rotating `JWT_SECRET` invalidates every previously issued JWT cookie - users will
   need to log in again.
