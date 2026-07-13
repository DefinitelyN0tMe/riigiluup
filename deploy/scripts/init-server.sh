#!/usr/bin/env bash
set -euo pipefail

# Bootstraps a fresh Ubuntu 24.04 VPS to run Politico.
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
mkdir -p /opt/politico/backups

# 6. Substitute the domain into nginx conf (in-place if the file was copied to /opt/politico/nginx/)
if [ -f /opt/politico/nginx/politico.conf ]; then
  sed -i "s/POLITICO_DOMAIN/${DOMAIN}/g" /opt/politico/nginx/politico.conf
fi

echo "Server ready. Next steps:"
echo "  1. Fill /opt/politico/.env with production secrets."
echo "  2. Bring the stack up:  cd /opt/politico && docker compose -f docker-compose.prod.yml up -d"
echo "  3. Issue a certificate:  bash scripts/issue-cert.sh $DOMAIN admin@example.com"
