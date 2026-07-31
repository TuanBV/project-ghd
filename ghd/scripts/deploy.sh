#!/usr/bin/env bash
# Picks the right Compose file automatically:
#   - if .env points DB_HOST at an external MySQL (a VPS host install) and it's
#     actually reachable, use compose.external-db.yaml
#   - otherwise (DB_HOST unset/left at the "mysql" default, or unreachable), fall
#     back to compose.yaml, which runs MySQL in its own container
#
# See docs/DOCKER.md section 11 for what the external host's MySQL needs
# (bind-address, grants, firewall) for the external-DB path to succeed.
set -euo pipefail
cd "$(dirname "$0")/.."

if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

db_host="${DB_HOST:-}"
db_port="${DB_PORT:-3306}"

use_external=false
if [ -n "$db_host" ] && [ "$db_host" != "mysql" ]; then
  if timeout 3 bash -c "cat < /dev/null > /dev/tcp/${db_host}/${db_port}" 2>/dev/null; then
    use_external=true
  fi
fi

if [ "$use_external" = true ]; then
  echo "External MySQL reachable at ${db_host}:${db_port} - using compose.external-db.yaml"
  exec docker compose -f compose.external-db.yaml up -d --build
else
  if [ -n "$db_host" ] && [ "$db_host" != "mysql" ]; then
    echo "DB_HOST=${db_host}:${db_port} is set but not reachable - falling back to containerized MySQL (compose.yaml)"
  else
    echo "No external DB_HOST configured - using containerized MySQL (compose.yaml)"
  fi
  exec docker compose -f compose.yaml up -d --build
fi
