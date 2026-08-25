#!/usr/bin/env bash
# Case 4 - Redis Memory Full. Fills the standalone `redis-memtest` container
# (compose.redis-lab.yml, maxmemory=20mb) until it's full, under a chosen eviction policy.
# Talks to `redis-memtest` only - never the app's real Redis - so this never touches
# production cache/session data.
#
# Uses `redis-benchmark` (bundled in the official redis image) to generate the writes in a
# single exec call instead of looping one `redis-cli`/`docker exec` per key: on this host each
# separate `docker compose exec` invocation costs ~0.5-0.8s of process-spawn overhead, so a
# few thousand single-key SETs that way takes tens of minutes. redis-benchmark drives the same
# writes with its own pipelined client in under a second - verified live: -n 8000 -d 4096 with
# maxmemory-policy=noeviction filled to ~18.3MB/20MB and started rejecting with "OOM command
# not allowed" in well under a second, and the same load with allkeys-lru completed all 15000
# SETs successfully (95k req/s) while evicting ~8500 older keys to stay under the limit.
#
# Usage:
#   ./scripts/redis-lab/memory-fill.sh noeviction
#   ./scripts/redis-lab/memory-fill.sh allkeys-lru
#
# Prereq: docker compose -f compose.yaml -f compose.redis-lab.yml up -d redis-memtest
set -euo pipefail

POLICY="${1:-noeviction}"
VALUE_SIZE_BYTES="${2:-4096}"
KEY_COUNT="${3:-8000}"

if [[ "$POLICY" != "noeviction" && "$POLICY" != "allkeys-lru" ]]; then
  echo "Usage: $0 [noeviction|allkeys-lru] [value-size-bytes] [key-count]" >&2
  exit 1
fi

RC() { docker compose exec -T redis-memtest redis-cli "$@"; }

echo "==> Setting maxmemory-policy=$POLICY on redis-memtest"
RC CONFIG SET maxmemory-policy "$POLICY" >/dev/null
RC FLUSHALL >/dev/null

echo "==> Writing $KEY_COUNT keys (~${VALUE_SIZE_BYTES}B each) via redis-benchmark"
docker compose exec -T redis-memtest redis-benchmark -q -t set \
  -n "$KEY_COUNT" -d "$VALUE_SIZE_BYTES" -r "$KEY_COUNT" || true

echo
echo "==> Result (policy=$POLICY, value size=${VALUE_SIZE_BYTES}B, attempted=$KEY_COUNT keys)"
echo
echo "-- INFO memory --"
RC INFO memory | grep -E "^(used_memory_human|maxmemory_human|maxmemory_policy):"
echo
echo "-- INFO stats (evicted_keys) --"
RC INFO stats | grep -E "^evicted_keys:"
echo
echo "-- DBSIZE (keys actually stored right now) --"
RC DBSIZE
