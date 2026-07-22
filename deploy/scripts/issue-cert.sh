#!/usr/bin/env bash
set -euo pipefail

# First-time Let's Encrypt certificate provisioning.
#
# Handles the chicken-and-egg between Nginx (needs cert files to load
# `riigiluup.conf`) and Certbot (needs an accessible HTTP endpoint to solve
# the ACME challenge). Strategy:
#   1) Temporarily point Nginx at riigiluup-bootstrap.conf (HTTP-only + serves
#      /.well-known/acme-challenge/).
#   2) Run certbot certonly --webroot.
#   3) Swap back to the full riigiluup.conf (with SSL) and reload.
#
# Usage: bash issue-cert.sh <domain> <email>

DOMAIN="${1:-}"
EMAIL="${2:-}"
if [ -z "$DOMAIN" ] || [ -z "$EMAIL" ]; then
  echo "Usage: issue-cert.sh <domain> <email>" >&2
  exit 1
fi

DEPLOY_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "${DEPLOY_DIR}"

BOOTSTRAP="./nginx/riigiluup-bootstrap.conf"
FULL="./nginx/riigiluup.conf"
LIVE="./nginx/.riigiluup.active.conf"

if [ ! -f "${BOOTSTRAP}" ]; then
  echo "Missing ${BOOTSTRAP} — is the deploy tree intact?" >&2
  exit 1
fi

# Substitute the domain sentinel in both configs (idempotent — grep first).
sed -i.bak "s/RIIGILUUP_DOMAIN/${DOMAIN}/g" "${BOOTSTRAP}"
sed -i.bak "s/RIIGILUUP_DOMAIN/${DOMAIN}/g" "${FULL}"
rm -f "${BOOTSTRAP}.bak" "${FULL}.bak"

echo "1) Backing up the full SSL config, then swapping nginx to bootstrap (HTTP-only)…"
# Keep a pristine copy of the full (SSL) config so we can restore it without git —
# rsync/tarball deploys have no .git to check out from.
FULL_BACKUP="${FULL}.full.bak"
cp "${FULL}" "${FULL_BACKUP}"
cp "${BOOTSTRAP}" "${LIVE}"
# Compose already mounts ./nginx/riigiluup.conf; overwrite it with the
# bootstrap contents for the duration of the challenge.
cp "${BOOTSTRAP}" "${FULL}"

# Ensure the stack is up. Recreate nginx so it re-mounts the current file inode
# (a bind-mounted file replaced by cp/sed is invisible to a running container).
docker compose -f docker-compose.prod.yml up -d api web db
docker compose -f docker-compose.prod.yml up -d --force-recreate nginx

echo "2) Requesting certificate from Let's Encrypt (apex + www)…"
docker compose -f docker-compose.prod.yml run --rm --entrypoint sh certbot -c "\
  certbot certonly --webroot -w /var/www/certbot \
    -d ${DOMAIN} -d www.${DOMAIN} \
    --email ${EMAIL} --agree-tos --non-interactive --expand"

echo "3) Restoring the full SSL nginx config and reloading…"
cp "${FULL_BACKUP}" "${FULL}"
# Recreate (not reload) so the container picks up the restored file inode.
docker compose -f docker-compose.prod.yml up -d --force-recreate nginx

echo "Done. HTTPS should be live at https://${DOMAIN}/ and https://www.${DOMAIN}/"
