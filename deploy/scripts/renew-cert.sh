#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# --entrypoint certbot is REQUIRED: the certbot service's entrypoint is an infinite renew+sleep
# loop, so a plain `run certbot renew ...` launches that loop and hangs forever (never reaching the
# nginx reload below, and leaking a stuck container). Overriding the entrypoint runs certbot once.
if ! docker compose -f docker-compose.prod.yml run --rm --entrypoint certbot certbot renew --webroot -w /var/www/certbot; then
  ./scripts/tg-notify.sh "🔴 RiigiLuup: certbot renew FAILED — TLS may lapse. Check the certificate manually."
  exit 1
fi
# Reload nginx to pick up any renewed cert; alert (don't fail) if the reload itself doesn't take.
docker compose -f docker-compose.prod.yml exec nginx nginx -s reload \
  || ./scripts/tg-notify.sh "🟠 RiigiLuup: cert renewed but nginx reload failed — reload manually."
