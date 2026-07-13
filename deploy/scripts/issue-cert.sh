#!/usr/bin/env bash
set -euo pipefail

DOMAIN="${1:-}"
EMAIL="${2:-}"
if [ -z "$DOMAIN" ] || [ -z "$EMAIL" ]; then
  echo "Usage: issue-cert.sh <domain> <email>" >&2
  exit 1
fi

cd "$(dirname "$0")/.."

# First pass — start Nginx WITHOUT SSL to serve the HTTP-01 challenge.
# We use a tiny bootstrap config that only listens on 80.
mkdir -p ./certbot-webroot

docker compose -f docker-compose.prod.yml run --rm --entrypoint sh certbot -c "\
  certbot certonly --webroot -w /var/www/certbot \
    -d ${DOMAIN} \
    --email ${EMAIL} --agree-tos --non-interactive \
"

echo "Certificate obtained. Reloading Nginx…"
docker compose -f docker-compose.prod.yml exec nginx nginx -s reload
