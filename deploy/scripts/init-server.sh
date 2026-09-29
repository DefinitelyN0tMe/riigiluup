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

# 6. (The nginx config names the production host directly; for another domain, edit
#    deploy/nginx/riigiluup.conf by hand. Nothing to substitute here any more.)

# 7. Scheduled backups + weekly cert renewal + weekly Docker build-cache prune (idempotent —
#    drop any prior line, then re-add). The prune keeps `docker compose --build` deploys from
#    accumulating unbounded build cache that fills the root disk (it grew to 21 GB / 84% once).
CRON_BACKUP="5 4 * * * cd /opt/riigiluup/deploy && ./scripts/backup.sh >> /var/log/riigiluup-backup.log 2>&1"
CRON_RENEW="0 3 * * 1 cd /opt/riigiluup/deploy && ./scripts/renew-cert.sh >> /var/log/riigiluup-cert.log 2>&1"
CRON_PRUNE="30 4 * * 0 docker builder prune -af --filter until=168h >> /var/log/riigiluup-prune.log 2>&1"
CRON_DISK="0 */6 * * * /opt/riigiluup/deploy/scripts/disk-alert.sh 85 >> /var/log/riigiluup-disk.log 2>&1"
# `|| true`: grep -Fv exits 1 when it filters out ALL lines (empty crontab on a fresh box, or a
# crontab holding only these managed lines). Under `set -euo pipefail` that would abort the subshell
# BEFORE the echoes and pipe an empty stream to `crontab -` — wiping the crontab / installing nothing.
( { crontab -l 2>/dev/null | grep -Fv 'scripts/backup.sh' | grep -Fv 'scripts/renew-cert.sh' | grep -Fv 'docker builder prune' | grep -Fv 'scripts/disk-alert.sh' || true; }; \
  echo "$CRON_BACKUP"; echo "$CRON_RENEW"; echo "$CRON_PRUNE"; echo "$CRON_DISK" ) | crontab -

# 8. Rotate the cron-appended logs so they can't grow unbounded.
cp /opt/riigiluup/deploy/logrotate/riigiluup /etc/logrotate.d/riigiluup 2>/dev/null \
  || echo "  (logrotate config not installed — copy deploy/logrotate/riigiluup to /etc/logrotate.d/ manually)"

echo "Server ready. Next steps:"
echo "  1. Fill /opt/riigiluup/deploy/.env with production secrets (cp deploy/.env.production.example deploy/.env)."
echo "  2. Issue a certificate:  cd /opt/riigiluup/deploy && bash scripts/issue-cert.sh $DOMAIN you@example.com"
echo "  3. Bring the stack up:  cd /opt/riigiluup/deploy && docker compose -f docker-compose.prod.yml up -d --build"
