#!/usr/bin/env bash
# Alert (once per run) if the root filesystem is at/above a usage threshold. Meant for cron.
# The disk filled to 84% once (Docker build cache); this catches a refill before it wedges Postgres.
# Usage: disk-alert.sh [threshold-percent]   (default 85)
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
THRESHOLD="${1:-85}"
USE="$(df --output=pcent / | tail -1 | tr -dc '0-9')"

if [ "${USE:-0}" -ge "$THRESHOLD" ]; then
  "$DIR/tg-notify.sh" "🟠 RiigiLuup: root disk ${USE}% full (threshold ${THRESHOLD}%). Check: df -h / ; docker system df ; docker builder prune -f"
fi
