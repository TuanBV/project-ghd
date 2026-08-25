#!/usr/bin/env bash
# Case 3 - Redis Timeout / Connection Error, via Toxiproxy (compose.redis-lab.yml).
# Runs toxiproxy-cli INSIDE the toxiproxy container (bundled in the image), talking to its
# own control API on localhost:8474 - nothing extra to install on the host.
#
# Usage:
#   ./scripts/redis-lab/toxiproxy-toxic.sh add-latency [ms]   # default 5000ms
#   ./scripts/redis-lab/toxiproxy-toxic.sh remove-latency
#   ./scripts/redis-lab/toxiproxy-toxic.sh down                # simulate Redis being unreachable
#   ./scripts/redis-lab/toxiproxy-toxic.sh up                  # restore
#   ./scripts/redis-lab/toxiproxy-toxic.sh status
set -euo pipefail

CLI() { docker compose exec -T toxiproxy toxiproxy-cli "$@"; }

case "${1:-}" in
  add-latency)
    MS="${2:-5000}"
    CLI toxic add redis_proxy -t latency -a latency="$MS" -n redis_latency
    echo "Added ${MS}ms latency toxic on redis_proxy. Also set REDIS_COMMAND_TIMEOUT low (e.g. 200ms) in .env and recreate the app to see the timeout+fallback."
    ;;
  remove-latency)
    CLI toxic remove redis_proxy -n redis_latency
    echo "Removed latency toxic."
    ;;
  down)
    CLI toggle redis_proxy
    echo "Toggled redis_proxy - if it was enabled, it's now disabled (connections refused), simulating Redis being down without touching the real `redis` container."
    ;;
  up)
    CLI toggle redis_proxy
    echo "Toggled redis_proxy again - should be back to enabled/pass-through."
    ;;
  status)
    CLI inspect redis_proxy
    ;;
  *)
    echo "Usage: $0 {add-latency [ms]|remove-latency|down|up|status}" >&2
    exit 1
    ;;
esac
