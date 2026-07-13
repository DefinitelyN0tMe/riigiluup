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
