#!/usr/bin/env bash
set -euo pipefail

# Bootstraps a fresh Ubuntu 24.04 VPS to run Riigiluup.
# Usage:  bash init-server.sh <domain>
#
# Idempotent — safe to re-run.

DOMAIN="${1:-}"
if [ -z "$DOMAIN" ]; then
  echo "Usage: init-server.sh <domain>" >&2
  exit 1
fi

# 1. System packages
export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y ca-certificates curl gnupg lsb-release ufw fail2ban

# 2. Docker
if ! command -v docker >/dev/null; then
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
  chmod a+r /etc/apt/keyrings/docker.asc
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" > /etc/apt/sources.list.d/docker.list
  apt-get update
  apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
fi

# 3. Firewall
ufw allow OpenSSH
ufw allow 80/tcp
ufw allow 443/tcp
ufw --force enable

# 4. Fail2ban (default sshd jail is enough for MVP)
systemctl enable --now fail2ban

# 5. Prepare deploy dir
mkdir -p /opt/riigiluup/backups

# 6. Substitute the domain into nginx conf. The repo layout keeps it under deploy/nginx/.
NGINX_CONF="$(dirname "$0")/../nginx/riigiluup.conf"
if [ -f "$NGINX_CONF" ]; then
  sed -i "s/RIIGILUUP_DOMAIN/${DOMAIN}/g" "$NGINX_CONF"
fi

# 7. Scheduled backups + weekly cert renewal + weekly Docker build-cache prune (idempotent —
#    drop any prior line, then re-add). The prune keeps `docker compose --build` deploys from
#    accumulating unbounded build cache that fills the root disk (it grew to 21 GB / 84% once).
CRON_BACKUP="5 4 * * * cd /opt/riigiluup/deploy && ./scripts/backup.sh >> /var/log/riigiluup-backup.log 2>&1"
CRON_RENEW="0 3 * * 1 cd /opt/riigiluup/deploy && ./scripts/renew-cert.sh >> /var/log/riigiluup-cert.log 2>&1"
CRON_PRUNE="30 4 * * 0 docker builder prune -f >> /var/log/riigiluup-prune.log 2>&1"
( crontab -l 2>/dev/null | grep -Fv 'scripts/backup.sh' | grep -Fv 'scripts/renew-cert.sh' | grep -Fv 'docker builder prune'; \
  echo "$CRON_BACKUP"; echo "$CRON_RENEW"; echo "$CRON_PRUNE" ) | crontab -

echo "Server ready. Next steps:"
echo "  1. Fill /opt/riigiluup/.env with production secrets."
echo "  2. Bring the stack up:  cd /opt/riigiluup && docker compose -f docker-compose.prod.yml up -d"
echo "  3. Issue a certificate:  bash scripts/issue-cert.sh $DOMAIN admin@example.com"
