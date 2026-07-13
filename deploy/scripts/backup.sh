#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

STAMP=$(date -u +%Y%m%dT%H%M%SZ)
OUT="./backups/politico-${STAMP}.dump"
mkdir -p ./backups

echo "Dumping to ${OUT}…"
docker compose -f docker-compose.prod.yml exec -T db \
  pg_dump -U "${POSTGRES_USER:?}" -d "${POSTGRES_DB:?}" -Fc > "${OUT}"

# Keep only the most recent 14 dumps.
ls -1t ./backups/politico-*.dump 2>/dev/null | tail -n +15 | xargs -r rm -f

echo "OK. Retained:"
ls -lh ./backups/politico-*.dump | head -n 14
