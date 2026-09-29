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

`init-server.sh` installs Docker, configures `ufw` (22/80/443) and `fail2ban`, and registers
the cron jobs (nightly Postgres backup, weekly TLS renewal, weekly build-cache prune, disk alert).
It is idempotent, safe to re-run. The nginx config names the production host directly; for
another domain edit `deploy/nginx/riigiluup.conf` by hand. Also consider:

- a 2 GB swap file (`fallocate -l 2G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile`, plus `/etc/fstab`);
- `PasswordAuthentication no` and `X11Forwarding no` in `/etc/ssh/sshd_config.d/`.

## 4. Production secrets

```bash
cp deploy/.env.production.example deploy/.env
chmod 600 deploy/.env
openssl rand -base64 24   # -> POSTGRES_PASSWORD, RIIGILUUP_ADMIN_PASSWORD, UMAMI_DB_PASSWORD
openssl rand -hex 32      # -> UMAMI_APP_SECRET
```

Fill every variable in `deploy/.env` (the example file documents each one): database, admin
fallback password, `SPRING_PROFILES_ACTIVE=prod`, the Google OAuth client and the admin
e-mail allow-list, Telegram alert bot, IndexNow key, and the four Umami variables. **Keep a copy
of `deploy/.env` outside the server** (a password manager): it is the only place these secrets live.

> **The `prod` startup guard will refuse to boot** if `POSTGRES_PASSWORD` or
> `RIIGILUUP_ADMIN_PASSWORD` are default/blank, or if `GOOGLE_OAUTH_CLIENT_ID`
> is empty. The Google OAuth redirect URI must be
> `https://riigiluup.ee/login/oauth2/code/google`.

## 5. Umami database (once)

Umami shares the Postgres container but uses its own role and database:

```bash
cd /opt/riigiluup/deploy
docker compose -f docker-compose.prod.yml up -d db
docker exec -it deploy-db-1 psql -U riigiluup -d riigiluup \
  -c "CREATE ROLE umami LOGIN PASSWORD '<UMAMI_DB_PASSWORD>';" \
  -c "CREATE DATABASE umami OWNER umami;"
```

After the stack is up, log in to Umami once through an SSH tunnel
(`ssh -L 3000:<umami container IP>:3000 root@<server>`), change the default admin password, add the
site, and put its website id into `UMAMI_WEBSITE_ID` and `frontend/index.html`.

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
docker compose -f docker-compose.prod.yml ps        # db, api, web healthy?
curl -s https://riigiluup.ee/healthz                 # {"status":"UP"}
```

### Load the data (first run only)

The database starts empty. Sign in to the admin panel (`https://riigiluup.ee/admin`, Google
account on the allow-list) and start the full backfill there. In `prod` the admin API accepts
only the OIDC session, not HTTP Basic. The source API is throttled to one request every 1.6 s, so
the full history takes a few hours; MPs and factions appear within minutes. The electoral
footprint and speech-to-bill links load themselves on the first boot.

## Routine deploy (how every change reaches production)

The server has a git checkout in `/opt/riigiluup`. Either `git pull` there (the repository is
public) or, from a workstation, ship a bundle:

```bash
git bundle create /tmp/rl.bundle master && scp /tmp/rl.bundle root@<server>:/tmp/rl.bundle
ssh root@<server> 'cd /opt/riigiluup && git fetch /tmp/rl.bundle master:refs/remotes/origin/master \
  && git checkout -B master origin/master'
```

Then on the server:

```bash
cd /opt/riigiluup/deploy
docker compose -f docker-compose.prod.yml up -d --build api web   # only what changed
# wait until `docker inspect -f '{{.State.Health.Status}}' deploy-api-1` says healthy
docker exec deploy-nginx-1 nginx -s reload     # edge re-resolves the api/web addresses
```

- **Do not restart the api between 04:25 and 05:00 (Tallinn):** the nightly import runs at 04:30.
- **A change to `deploy/nginx/*.conf` needs a recreate, not a reload** (the files are single-file
  bind mounts, so a reload keeps the old content). Validate first, on the compose network:

  ```bash
  docker run --rm --network deploy_default \
    -v /opt/riigiluup/deploy/nginx/nginx.conf:/etc/nginx/nginx.conf:ro \
    -v /opt/riigiluup/deploy/nginx/riigiluup.conf:/etc/nginx/conf.d/riigiluup.conf:ro \
    -v deploy_letsencrypt:/etc/letsencrypt:ro nginx:1.28-alpine \
    sh -c "touch /etc/nginx/.htpasswd; nginx -t"
  docker compose -f docker-compose.prod.yml up -d --force-recreate nginx
  ```
- Flyway applies new migrations on api start; a bulk data migration must begin with
  `SET LOCAL statement_timeout = '0';`.
- Deploy only a commit whose CI run is green.

## Maintenance

- **Backups:** `deploy/scripts/backup.sh` runs nightly via cron, verifies each dump and keeps 14.
  They are on the same disk as the database, so also copy them off the server (another host,
  a storage box, or Hetzner Backups). The Umami database is not in these dumps; dump it separately
  if its history matters.
- **Restore:** stop the api first (`docker compose -f docker-compose.prod.yml stop api`), then
  `deploy/scripts/restore.sh <dump>`, then start the api again. Test a restore into a throwaway
  Postgres container now and then.
- **TLS renewal:** `deploy/scripts/renew-cert.sh` runs weekly via cron and reloads nginx.
- **OS updates:** unattended-upgrades installs security updates; reboot in a quiet hour when
  `/var/run/reboot-required` exists, then check `docker ps`, `/healthz` and reload nginx and web.
- **Monitoring / alerts:** the Telegram bot reports api startup, any import step that fails (an
  exception or a FAILED run), and, twice a day, any import job that has not landed data within
  about twice its schedule. `disk-alert.sh` warns at >85% disk; `backup.sh` alerts on a bad dump.
  UptimeRobot watches `/healthz` (edge -> api -> DB). The weekly
  `docker builder prune -af --filter until=168h` keeps build cache from filling the disk.

## Rollback / incident response

If a deploy breaks the site (api boot-loops on a bad migration, a bad build, etc.):

1. **Diagnose:** `docker compose -f deploy/docker-compose.prod.yml logs --tail=100 api`
   (Flyway/boot errors), `docker ps` (health), `curl -s https://<domain>/healthz`
   (`{"status":"UP"}` = api + DB alive).
2. **Roll back the code:** on the server, `cd /opt/riigiluup && git checkout <previous-good-sha>`
   then `docker compose -f deploy/docker-compose.prod.yml up -d --build api web`, then reload
   nginx. The web/nginx edge is decoupled from api health, so the static SPA and TLS
   renewal stay up even while the api is down — visitors get a degraded shell, not a blank page.
3. **A bad migration can't be auto-reverted.** Either fix-forward (add a corrective
   migration) and redeploy, or restore the last good dump: `deploy/scripts/restore.sh
   backups/<dump>` then redeploy the prior code. Flyway is forward-only; never hand-edit
   `flyway_schema_history`.
4. **Disk full:** `df -h /` + `docker system df`; `docker builder prune -af` frees
   build cache. Disk >85% is alerted; memory is watched only through container health checks.

## Security posture (built in)

- `prod` refuses to start with default/blank DB or admin passwords, or without OIDC.
- Admin panel: Google OIDC + email allow-list; per-IP API rate limits (240/min default,
  files 300/min, analytics and stenogram search 120/min); crawler-rendered pages and the sitemap
  are rate-limited in nginx; the file proxy serves only known MP portraits.
- nginx: TLS 1.2/1.3, HSTS, strict CSP, anti-clickjacking, `server_tokens off`,
  actuator reduced to `/health`. The API port is not published outside the Docker
  network — nginx is the only ingress.
