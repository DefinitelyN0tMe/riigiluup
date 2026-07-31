# Deploying Riigiluup (Hetzner Cloud + Ubuntu 24.04)

End-to-end runbook for a fresh production deploy. The stack is Docker Compose
(Postgres 16 + Spring Boot API + nginx serving the built SPA + certbot).

> **Order matters.** Point DNS at the server *before* issuing the TLS certificate,
> and fill in the production secrets *before* starting the stack — the app refuses
> to boot in the `prod` profile with default/blank passwords or without Google OIDC.

---

## 1. Provision the server

- **Hetzner Cloud**, type **CX22** (2 vCPU / 4 GB) or **CPX21**, image **Ubuntu 24.04**.
- Region near Estonia (Helsinki / Falkenstein / Nuremberg).
- Add your **SSH public key** at creation (no password login).
- Enable **Backups**.
- **Hetzner Cloud Firewall** (panel, in front of the server): inbound allow only
  `22`, `80`, `443`; deny the rest. This duplicates the on-host `ufw`.

## 2. DNS

Point the domain at the server's IP **before** step 5:

```
A   riigiluup.ee       -> <server IP>
A   www.riigiluup.ee   -> <server IP>
```

## 3. Bootstrap the host

```bash
ssh root@<server IP>
git clone https://github.com/DefinitelyN0tMe/riigiluup.git /opt/riigiluup
cd /opt/riigiluup
bash deploy/scripts/init-server.sh riigiluup.ee
```

`init-server.sh` installs Docker, configures `ufw` (22/80/443) and `fail2ban`,
substitutes the domain into the nginx config, and registers cron jobs for nightly
Postgres backups and weekly TLS renewal. It is idempotent — safe to re-run.

## 4. Production secrets

```bash
cp deploy/.env.production.example deploy/.env
# generate strong values:
openssl rand -base64 24   # -> POSTGRES_PASSWORD
openssl rand -base64 24   # -> RIIGILUUP_ADMIN_PASSWORD
```

Edit `deploy/.env`:

```
POSTGRES_DB=riigiluup
POSTGRES_USER=riigiluup
POSTGRES_PASSWORD=<generated>
RIIGILUUP_ADMIN_USERNAME=admin
RIIGILUUP_ADMIN_PASSWORD=<generated>          # OIDC fallback; must be non-default
RIIGILUUP_DOMAIN=riigiluup.ee
SPRING_PROFILES_ACTIVE=prod
RIIGILUUP_ADMIN_ALLOWED_EMAILS=you@example.com   # your Google account(s)
GOOGLE_OAUTH_CLIENT_ID=<from Google Cloud Console>
GOOGLE_OAUTH_CLIENT_SECRET=<from Google Cloud Console>
```

> **The `prod` startup guard will refuse to boot** if `POSTGRES_PASSWORD` or
> `RIIGILUUP_ADMIN_PASSWORD` are default/blank, or if `GOOGLE_OAUTH_CLIENT_ID`
> is empty. The Google OAuth redirect URI must be
> `https://riigiluup.ee/login/oauth2/code/google`.

## 5. Private-beta gate (recommended until launch)

One shared password gates the **entire** site (SPA + API + admin) at nginx:

```bash
apt-get install -y apache2-utils      # provides htpasswd
htpasswd -Bc deploy/nginx/.htpasswd beta   # prompts for the shared beta password
```

The `.htpasswd` file is gitignored and bind-mounted read-only. Leave it in place
while testing on the real domain; remove it at public launch (step 8).

## 6. TLS certificate (Let's Encrypt)

DNS must already resolve to this server.

```bash
cd /opt/riigiluup/deploy
bash scripts/issue-cert.sh riigiluup.ee you@example.com
```

This temporarily serves an HTTP-only bootstrap nginx to solve the ACME challenge,
obtains the cert via certbot webroot, then swaps in the full HTTPS config.

## 7. Start the stack

```bash
cd /opt/riigiluup/deploy
docker compose -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml ps        # all healthy?
curl -sk -u beta:<beta-pass> https://riigiluup.ee/actuator/health   # {"status":"UP"}
```

### Load the data (first run only)

The database starts empty. Kick off the full backfill (throttled ~1 rps, a few
hours; MPs/factions appear within minutes):

```bash
curl -u beta:<beta-pass> -u admin:<admin-pass> -X POST \
  "https://riigiluup.ee/api/v1/admin/backfill/full?from=2023-04-01&kinds=ALL"
# progress:
curl -u beta:<beta-pass> "https://riigiluup.ee/api/v1/admin/backfill/full/latest"
```

Committee memberships (`GROUPS`/`DETAILS`) usually import on the first pass; the
orchestrator now retries a step once after a 90 s cooldown if it hits a transient
429, so a stray rate-limit no longer costs the whole step.

### Electoral footprint & speech-to-bill links (automatic)

The **Valimised** section on MP profiles (RK/EP/KOV campaigns from
opendata.valimised.ee) and the **Arutelu ja stenogrammid** section on bill pages
load themselves on the **first boot** after this build deploys: a self-gating
startup task builds the speech-to-bill links for existing speeches and imports the
EP/KOV footprint, then no-ops on later boots. New sittings link on ingest, so no
recurring action is needed. If you want to trigger them by hand (e.g. after adding
a newly published election), the admin endpoints are:

```bash
curl -u beta:<beta-pass> -u admin:<admin-pass> -X POST \
  "https://riigiluup.ee/api/v1/admin/import/elections"
curl -u beta:<beta-pass> -u admin:<admin-pass> -X POST \
  "https://riigiluup.ee/api/v1/admin/import/link-speeches-to-bills"
```

## 8. Test, then go public

1. Verify every section on the live domain over HTTPS.
2. Test the admin login via Google (your allow-listed email).
3. Go fully public: remove the two `auth_basic` lines from
   `deploy/nginx/riigiluup.conf`, restore `frontend/public/robots.txt` to allow
   indexing, then reload nginx:
   ```bash
   docker compose -f docker-compose.prod.yml exec web nginx -s reload
   ```

## Maintenance

- **Backups:** `deploy/scripts/backup.sh` runs nightly via cron (installed in
  step 3); Hetzner server snapshots are the second line.
- **Restore:** `deploy/scripts/restore.sh <dump>`.
- **TLS renewal:** `deploy/scripts/renew-cert.sh` runs weekly via cron.
- **Update the app:** `git pull` in `/opt/riigiluup`, then
  `docker compose -f deploy/docker-compose.prod.yml up -d --build`. The database
  volume is preserved; Flyway applies any new migrations on start.

## Security posture (built in)

- `prod` refuses to start with default/blank DB or admin passwords, or without OIDC.
- Admin panel: Google OIDC + email allow-list; per-IP rate limits (default 60/min,
  files 300/min, analytics 20/min); file proxy bounded by a semaphore.
- nginx: TLS 1.2/1.3, HSTS, strict CSP, anti-clickjacking, `server_tokens off`,
  actuator reduced to `/health`. The API port is not published outside the Docker
  network — nginx is the only ingress.
