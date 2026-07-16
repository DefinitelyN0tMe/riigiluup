#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

# Compose reads ./.env automatically; a bare shell (cron) does not — source it so the
# ${POSTGRES_*} vars below resolve instead of aborting on `${VAR:?}`.
[ -f ./.env ] && { set -a; . ./.env; set +a; }

STAMP=$(date -u +%Y%m%dT%H%M%SZ)
OUT="./backups/riigiluup-${STAMP}.dump"
mkdir -p ./backups

echo "Dumping to ${OUT}…"
docker compose -f docker-compose.prod.yml exec -T db \
  pg_dump -U "${POSTGRES_USER:?}" -d "${POSTGRES_DB:?}" -Fc > "${OUT}"

# Keep only the most recent 14 dumps. Consider copying ./backups off-box (rsync/object storage)
# for real disaster recovery — retention here shares the DB's disk.
ls -1t ./backups/riigiluup-*.dump 2>/dev/null | tail -n +15 | xargs -r rm -f

echo "OK. Retained:"
ls -lh ./backups/riigiluup-*.dump | head -n 14
