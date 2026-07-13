# Politico

Civic-tech platform aggregating Estonian Riigikogu open data into
public MP profiles, voting histories, and legislation views.

## Running locally

```
docker compose up --build
```

Backend: http://localhost:8080
Frontend: http://localhost:5173
Postgres: localhost:5432 (user: politico / pass: politico)

## Stack

Spring Boot 3, Java 21, PostgreSQL 16, Flyway.
React 18, Vite, TypeScript, Tailwind CSS.

## Data source

`api.riigikogu.ee` — CC BY-SA 3.0.

## Production deployment

Minimum host: Ubuntu 24.04 LTS, 2 GB RAM, 2 vCPU, EU region, one public IPv4, one domain pointed at the IP.

1. Copy this repo to `/opt/politico` on the VPS (SCP, rsync, or `git clone`).
2. Run `bash /opt/politico/deploy/scripts/init-server.sh <domain>` as root once. Installs Docker, opens firewall, substitutes the domain into the Nginx config.
3. Copy `deploy/.env.production.example` to `deploy/.env` and fill in strong passwords.
4. `cd /opt/politico/deploy && docker compose -f docker-compose.prod.yml up -d`.
5. `bash scripts/issue-cert.sh <domain> <email>` — obtains the first Let's Encrypt cert. The certbot container handles automatic renewal from then on.
6. Trigger first ingestion via the admin dashboard at `https://<domain>/admin`.

Backups:
- `bash /opt/politico/deploy/scripts/backup.sh` — writes a `pg_dump -Fc` under `deploy/backups/`. Keeps the 14 most recent files. Wire this into cron: `5 4 * * * cd /opt/politico/deploy && ./scripts/backup.sh >> /var/log/politico-backup.log 2>&1`.
- `bash /opt/politico/deploy/scripts/restore.sh <path/to/dump>` — restores a single dump.
