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
if ! docker compose -f docker-compose.prod.yml exec -T db \
     pg_dump -U "${POSTGRES_USER:?}" -d "${POSTGRES_DB:?}" -Fc > "${OUT}"; then
  rm -f "${OUT}"
  ./scripts/tg-notify.sh "🔴 RiigiLuup: pg_dump FAILED — no backup written ($(date -u +%FT%TZ))."
  [ -n "${HEALTHCHECK_BACKUP_URL:-}" ] && curl -fsS --max-time 10 "${HEALTHCHECK_BACKUP_URL}/fail" >/dev/null 2>&1 || true
  exit 1
fi

# Integrity check: a truncated/failed dump can look like a valid retained file and rotate out good
# ones. A real custom-format dump is non-trivial in size and lists cleanly.
SIZE=$(stat -c%s "${OUT}" 2>/dev/null || echo 0)
if [ "${SIZE}" -lt 100000 ] \
   || ! docker compose -f docker-compose.prod.yml exec -T db pg_restore --list < "${OUT}" >/dev/null 2>&1; then
  rm -f "${OUT}"
  ./scripts/tg-notify.sh "🔴 RiigiLuup: backup verify FAILED (size=${SIZE}B or unreadable) — dump discarded ($(date -u +%FT%TZ))."
  [ -n "${HEALTHCHECK_BACKUP_URL:-}" ] && curl -fsS --max-time 10 "${HEALTHCHECK_BACKUP_URL}/fail" >/dev/null 2>&1 || true
  exit 1
fi

# Keep only the most recent 14 dumps. Consider copying ./backups off-box (rsync/object storage)
# for real disaster recovery — retention here shares the DB's disk.
ls -1t ./backups/riigiluup-*.dump 2>/dev/null | tail -n +15 | xargs -r rm -f

# Umami's database (visit history, not re-derivable from any source). Best-effort: a failure here
# is reported but does not fail the main backup. Superuser POSTGRES_USER can dump it.
UMAMI_OUT="./backups/umami-${STAMP}.dump"
if docker compose -f docker-compose.prod.yml exec -T db psql -U "${POSTGRES_USER:?}" -d "${POSTGRES_DB:?}" -tAc \
     "select 1 from pg_database where datname='umami'" 2>/dev/null | grep -q 1; then
  if ! docker compose -f docker-compose.prod.yml exec -T db \
       pg_dump -U "${POSTGRES_USER:?}" -d umami -Fc > "${UMAMI_OUT}"; then
    rm -f "${UMAMI_OUT}"
    ./scripts/tg-notify.sh "🟠 RiigiLuup: Umami pg_dump failed ($(date -u +%FT%TZ)); main backup is fine."
  fi
  ls -1t ./backups/umami-*.dump 2>/dev/null | tail -n +15 | xargs -r rm -f
fi

# Dead-man ping on success (optional): if HEALTHCHECK_BACKUP_URL is set, a missing ping alerts you
# that the backup cron didn't run at all — the one thing Telegram-on-failure can't tell you.
[ -n "${HEALTHCHECK_BACKUP_URL:-}" ] && curl -fsS --max-time 10 "${HEALTHCHECK_BACKUP_URL}" >/dev/null 2>&1 || true

echo "OK. Retained:"
ls -lh ./backups/riigiluup-*.dump | head -n 14
