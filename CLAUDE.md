# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

FinTrack is a personal finance tracking application: account management, transaction tracking, budgets with spending alerts, a dashboard, CSV import/export, and PDF reports. Currently backend-only (Spring Boot); all code lives under `backend/`.

## Commands

All commands run from `backend/`.

```bash
./gradlew build              # compile + test + package
./gradlew bootRun            # run locally (needs Postgres/Redis — see below)
./gradlew test               # run tests
./gradlew test --tests "com.fintrack.SomeTest"   # single test
```

There is no Makefile or scripts directory — Gradle wrapper is the only entry point.

**Note:** there are currently no test files anywhere in `backend/src/test` despite `spring-boot-starter-test` and `spring-security-test` being on the classpath. Don't assume test coverage exists for a class just because it seems testable.

### Running with Docker

```bash
cd backend
docker compose up
```

Spins up Postgres (`5433:5432`), Redis (`6379:6379`), and the backend (`8080`). The backend service reads config via `env_file: .env` (see `.env.example` for the required keys — `APP_JWT_SECRET`, `MAILGUN_API_KEY`, etc.). `application.yml` and `.env` are gitignored; copy `.env.example` to `.env` and fill in real values before running.

The `DockerFile` (note: capital F, non-standard filename) is a two-stage build producing a non-root `eclipse-temurin:21-jre-alpine` runtime image with `-Dspring.profiles.active=prod`.

## Architecture

Java 21, Spring Boot 3.3.4, Gradle Kotlin DSL. Strict layering:

```
web/controller → service/<domain> → repository → domain/model (JPA entities)
```

- **`web/controller`** — REST controllers, one per domain area (Auth, User, Account, Transaction, Category, Budget, Dashboard, Report). Use `@AuthenticationPrincipal UserPrincipal principal` to get the current user; the user id is passed explicitly into service calls rather than relying on implicit tenant context.
- **`web/dto/request` / `web/dto/response`** — DTOs are mandatory at the API boundary; entities never leak through controllers.
- **`service/{auth,transaction,account,budget,dashboard,report,notification}`** — business logic, one subpackage per domain. Notable non-CRUD services: `CsvImportService`/`CsvExportService` (Apache Commons CSV + OpenCSV), `PdfExportService` (OpenPDF), `EmailService` (Mailgun, Thymeleaf templates under `resources/templates/email/`).
- **`domain/model`** / **`domain/enums`** — JPA entities (User, Account, Transaction, Category, Budget, BudgetAlertLog, RefreshToken, NotificationPrefs) and enum types.
- **`repository`** — Spring Data JPA repositories.
- **`security`** — `JwtService` issues/validates HMAC-signed JWTs (key length must satisfy jjwt's minimum for the algorithm it auto-selects — see `JwtService`); `JwtAuthFilter` is a `OncePerRequestFilter` that reads the `Authorization: Bearer` header, validates the token, loads the user, and sets `UserPrincipal` on `SecurityContextHolder`.
- **`config`** — `SecurityConfig` (stateless JWT filter chain, CSRF disabled, CORS allowed for `localhost:5173`/`3000`, `/api/v1/auth/**` + actuator health + Swagger UI are public, everything else requires auth), `AsyncConfig` (`@EnableAsync` with a custom `ThreadPoolTaskExecutor`, core 2 / max 10 / queue 50), `SwaggerConfig` (springdoc OpenAPI at `/v3/api-docs`, `/swagger-ui/**`).
- **`exception`** — `GlobalExceptionHandler` (`@RestControllerAdvice`) maps `ResourceNotFoundException`→404, `BadRequestException`→400, `UnauthorisedException`→401, validation errors→400 with a field-error map, and a catch-all→500 with no stack trace leaked.
- **`scheduler`** — `BudgetAlertScheduler` runs hourly (`@Scheduled(cron = "0 0 * * * *")`), checks budgets vs. actual spend, sends 80%/100% threshold alerts via `EmailService`, and dedupes sends via `BudgetAlertLogRepository`.

### Database

Flyway-managed, migrations in `backend/src/main/resources/db/migration/`, applied automatically on boot. Core tables (from `V1__init_schema.sql` onward): `users`, `refresh_tokens`, `categories`, `accounts`, `transactions`, `budgets`, `notification_prefs`. Migrations are strictly additive/forward-only (`V3`/`V4` are follow-up column-type fixes) — never edit an already-applied migration file, add a new `V{n}__*.sql` instead.

### Redis

Configured as a dependency and in `docker-compose.yml`/`application.yml` (host/port), but not currently used anywhere in code — no `@Cacheable`, `RedisTemplate`, or rate-limiting logic exists yet. Treat it as provisioned infrastructure, not an active caching layer.

## Configuration

`application.yml` and `.env` are both gitignored (secrets live only locally/in the deploy environment). `application.yml` resolves every property from an env var via `${VAR:default}` placeholders; `APP_JWT_SECRET` and `MAILGUN_API_KEY` have no default, so the app fails fast on boot if they're unset rather than running with a weak/missing secret. `application-prod.yml` layers on prod-only overrides (JSON console logging, `ddl-auto: validate` — Hibernate must never auto-modify schema in prod, Flyway owns schema changes).
