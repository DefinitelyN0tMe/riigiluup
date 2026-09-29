#!/usr/bin/env bash
set -euo pipefail

FILE="${1:-}"
if [ -z "$FILE" ] || [ ! -f "$FILE" ]; then
  echo "Usage: restore.sh <path/to/riigiluup-XXXX.dump>" >&2
  exit 1
fi

cd "$(dirname "$0")/.."

# Source ./.env so ${POSTGRES_*} resolve when run from a bare shell (compose loads it, a shell doesn't).
[ -f ./.env ] && { set -a; . ./.env; set +a; }

# Stop the api for the duration: pg_restore --clean drops and recreates every table, and the
# importers or live requests must not write in between. --single-transaction makes the restore
# all-or-nothing: if anything fails, the database is left exactly as it was, not half-dropped.
echo "Stopping api…"
docker compose -f docker-compose.prod.yml stop api

echo "Restoring $FILE into database…"
if docker compose -f docker-compose.prod.yml exec -T db \
     pg_restore -U "${POSTGRES_USER:?}" -d "${POSTGRES_DB:?}" --clean --if-exists --single-transaction < "$FILE"; then
  echo "Restore OK."
else
  echo "Restore FAILED; the database was rolled back to its previous state." >&2
fi

echo "Starting api…"
docker compose -f docker-compose.prod.yml start api
echo "Done. Reload nginx once the api is healthy:  docker exec deploy-nginx-1 nginx -s reload"
