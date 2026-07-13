#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose -f docker-compose.prod.yml run --rm certbot renew --webroot -w /var/www/certbot
docker compose -f docker-compose.prod.yml exec nginx nginx -s reload
