#!/usr/bin/env bash
# Send a one-line ops alert to the owner's Telegram bot. Credentials come from deploy/.env
# (gitignored: TELEGRAM_BOT_TOKEN, TELEGRAM_ALERT_CHAT_ID). No-op if either is unset, so this is
# safe to call from any cron/script. Best-effort: never fails the caller.
# Usage: tg-notify.sh "message text"
set -euo pipefail

ENV_FILE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/.env"
TOKEN=""; CHAT=""
if [ -f "$ENV_FILE" ]; then
  TOKEN="$(grep -E '^TELEGRAM_BOT_TOKEN=' "$ENV_FILE" 2>/dev/null | head -1 | cut -d= -f2- || true)"
  CHAT="$(grep -E '^TELEGRAM_ALERT_CHAT_ID=' "$ENV_FILE" 2>/dev/null | head -1 | cut -d= -f2- || true)"
fi
if [ -z "$TOKEN" ] || [ -z "$CHAT" ]; then
  exit 0
fi

curl -s --max-time 10 "https://api.telegram.org/bot${TOKEN}/sendMessage" \
  --data-urlencode "chat_id=${CHAT}" \
  --data-urlencode "text=${1:-(empty alert)}" \
  --data-urlencode "disable_web_page_preview=true" >/dev/null 2>&1 || true
