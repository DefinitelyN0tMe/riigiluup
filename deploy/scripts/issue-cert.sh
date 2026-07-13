#!/usr/bin/env bash
set -euo pipefail

# First-time Let's Encrypt certificate provisioning.
#
# Handles the chicken-and-egg between Nginx (needs cert files to load
# `politico.conf`) and Certbot (needs an accessible HTTP endpoint to solve
# the ACME challenge). Strategy:
#   1) Temporarily point Nginx at politico-bootstrap.conf (HTTP-only + serves
#      /.well-known/acme-challenge/).
#   2) Run certbot certonly --webroot.
#   3) Swap back to the full politico.conf (with SSL) and reload.
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

BOOTSTRAP="./nginx/politico-bootstrap.conf"
FULL="./nginx/politico.conf"
LIVE="./nginx/.politico.active.conf"

if [ ! -f "${BOOTSTRAP}" ]; then
  echo "Missing ${BOOTSTRAP} — is the deploy tree intact?" >&2
  exit 1
fi

# Substitute the domain sentinel in both configs (idempotent — grep first).
sed -i.bak "s/POLITICO_DOMAIN/${DOMAIN}/g" "${BOOTSTRAP}"
sed -i.bak "s/POLITICO_DOMAIN/${DOMAIN}/g" "${FULL}"
rm -f "${BOOTSTRAP}.bak" "${FULL}.bak"

echo "1) Swapping nginx to bootstrap (HTTP-only) config…"
cp "${BOOTSTRAP}" "${LIVE}"
# Compose already mounts ./nginx/politico.conf; overwrite it with the
# bootstrap contents for the duration of the challenge.
cp "${BOOTSTRAP}" "${FULL}"

# Ensure the stack is up (or start it if not).
docker compose -f docker-compose.prod.yml up -d nginx api web db

echo "2) Reloading nginx with bootstrap config…"
docker compose -f docker-compose.prod.yml exec nginx nginx -s reload

echo "3) Requesting certificate from Let's Encrypt…"
docker compose -f docker-compose.prod.yml run --rm --entrypoint sh certbot -c "\
  certbot certonly --webroot -w /var/www/certbot \
    -d ${DOMAIN} \
    --email ${EMAIL} --agree-tos --non-interactive"

echo "4) Restoring full SSL nginx config…"
git -C "${DEPLOY_DIR}/.." checkout -- "deploy/nginx/politico.conf" 2>/dev/null || {
  echo "  (git not available or repo not clean — regenerating politico.conf from LIVE checkpoint)"
  cp "${LIVE}" "${FULL}"
  echo "  MANUAL STEP REQUIRED: politico.conf currently holds bootstrap content."
  echo "  Restore the full SSL version from your source tree, then run:"
  echo "  docker compose -f docker-compose.prod.yml exec nginx nginx -s reload"
  exit 0
}
sed -i "s/POLITICO_DOMAIN/${DOMAIN}/g" "${FULL}"
docker compose -f docker-compose.prod.yml exec nginx nginx -s reload

echo "Done. HTTPS should be live at https://${DOMAIN}/"
