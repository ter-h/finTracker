# FinTrack

A personal finance tracking application that allows users to manage accounts,
track transactions, create budgets, and receive spending alerts.

## Features

- User authentication with JWT
- Account management
- Transaction tracking
- Budget creation with threshold-based email alerts (80% / 100% of budget)
- Spending analysis dashboard
- CSV import/export
- PDF reports

## Tech Stack

Backend:
- Java 21, Spring Boot 3
- PostgreSQL with Flyway migrations
- Redis (provisioned; not yet used by application code)
- Docker / Docker Compose

## Prerequisites

- Docker and Docker Compose (recommended path), **or**
- JDK 21 and a local PostgreSQL 16 instance if running without Docker

## Getting Started

### 1. Configure environment variables

Copy the example env file and fill in real values:

```bash
cd backend
cp .env.example .env
```

At minimum, set `APP_JWT_SECRET` (generate one with `openssl rand -base64 64`). `MAILGUN_API_KEY`/`MAILGUN_DOMAIN` are only required if you need email alerts to actually send.

`.env` and `application.yml` are gitignored and must never be committed — see [Configuration](#configuration) below.

### 2. Run with Docker

```bash
cd backend
docker compose up
```

This starts Postgres (`localhost:5433`), Redis (`localhost:6379`), and the backend (`localhost:8080`). Flyway migrations run automatically on startup.

### 3. Run locally without Docker

```bash
cd backend
./gradlew bootRun
```

Requires Postgres reachable at the URL in your env vars, and `APP_JWT_SECRET` exported in your shell (it has no default).

## Configuration

All configuration is resolved from environment variables via `backend/src/main/resources/application.yml`, which is not committed to the repo. `backend/.env.example` documents every key; copy it to `.env` for local/Docker use. `APP_JWT_SECRET` and `MAILGUN_API_KEY` intentionally have no defaults, so the app fails to start rather than running with a missing or weak secret.

## API Documentation

Interactive API docs (Swagger UI) are available once the app is running, at:

- `http://localhost:8080/swagger-ui/index.html`
- Raw OpenAPI spec: `http://localhost:8080/v3/api-docs`

All endpoints are versioned under `/api/v1/`: `auth`, `users`, `accounts`, `transactions`, `categories`, `budgets`, `dashboard`, `reports`. Every route except `/api/v1/auth/**`, actuator health, and Swagger requires a `Authorization: Bearer <jwt>` header.

See [`docs/API.md`](docs/API.md) for auth flow, error response shapes, and pagination conventions not captured by the OpenAPI schema.

## Database

Schema is managed entirely through Flyway migrations in `backend/src/main/resources/db/migration/`, applied automatically on boot. Never edit an already-applied migration — add a new `V{n}__description.sql` instead. In production, Hibernate's `ddl-auto` is set to `validate` (see `application-prod.yml`) so schema drift is caught rather than silently auto-applied.

## Testing

```bash
cd backend
./gradlew test
```

## Production Build

```bash
cd backend
docker build -t fintrack-backend -f DockerFile .
```

Produces a non-root runtime image running with the `prod` Spring profile (`application-prod.yml`), which enables JSON structured logging and restricts actuator exposure to `health`, `metrics`, and `prometheus`.
