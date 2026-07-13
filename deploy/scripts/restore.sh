#!/usr/bin/env bash
set -euo pipefail

FILE="${1:-}"
if [ -z "$FILE" ] || [ ! -f "$FILE" ]; then
  echo "Usage: restore.sh <path/to/politico-XXXX.dump>" >&2
  exit 1
fi

cd "$(dirname "$0")/.."

echo "Restoring $FILE into database…"
docker compose -f docker-compose.prod.yml exec -T db \
  pg_restore -U "${POSTGRES_USER:?}" -d "${POSTGRES_DB:?}" --clean --if-exists < "$FILE"

echo "Done."
