# Riigiluup (riigiluup.ee)

Civic-tech platform aggregating Estonian Riigikogu open data into
public MP profiles, voting histories, and legislation views.

## Running locally

```
docker compose up --build
```

Backend (docker): http://localhost:18080
Frontend: http://localhost:5173
Postgres (docker): localhost:15432

Local development credentials are defined in the Docker Compose file. Running the
backend directly with `./gradlew bootRun` instead serves on port 8081.

## Stack

Spring Boot 3, Java 21, PostgreSQL 16, Flyway.
React 18, Vite, TypeScript, Tailwind CSS.

## Data source

`api.riigikogu.ee` — CC BY-SA 3.0.

## Deployment

Containerised with Docker Compose. Production configuration and helper scripts
(server setup, TLS, backups) live in the `deploy/` folder.
