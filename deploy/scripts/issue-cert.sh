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

echo "1) Swapping nginx to bootstrap (HTTP-only) config…"
cp "${BOOTSTRAP}" "${LIVE}"
# Compose already mounts ./nginx/riigiluup.conf; overwrite it with the
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
git -C "${DEPLOY_DIR}/.." checkout -- "deploy/nginx/riigiluup.conf" 2>/dev/null || {
  echo "  (git not available or repo not clean — regenerating riigiluup.conf from LIVE checkpoint)"
  cp "${LIVE}" "${FULL}"
  echo "  MANUAL STEP REQUIRED: riigiluup.conf currently holds bootstrap content."
  echo "  Restore the full SSL version from your source tree, then run:"
  echo "  docker compose -f docker-compose.prod.yml exec nginx nginx -s reload"
  exit 0
}
sed -i "s/RIIGILUUP_DOMAIN/${DOMAIN}/g" "${FULL}"
docker compose -f docker-compose.prod.yml exec nginx nginx -s reload

echo "Done. HTTPS should be live at https://${DOMAIN}/"
