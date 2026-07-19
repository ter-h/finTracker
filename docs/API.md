# API Reference

This document covers conventions the live OpenAPI schema doesn't spell out: auth flow, error shapes, and pagination. For the full, always-current list of endpoints/request-response schemas, run the app and use:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Raw OpenAPI spec: `http://localhost:8080/v3/api-docs`

All routes are versioned under `/api/v1/`.

## Authentication

`POST /api/v1/auth/register` and `POST /api/v1/auth/login` are the only public routes besides actuator health and Swagger. Every other endpoint requires:

```
Authorization: Bearer <accessToken>
```

Both auth endpoints return the same shape:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "user": { "...": "UserResponse fields" }
}
```

`register` returns `201 Created`, `login` returns `200 OK`. There is no refresh-token endpoint currently exposed, even though a `refresh_tokens` table exists in the schema.

The JWT identifies the caller; controllers resolve the current user via `@AuthenticationPrincipal`, and every service call is scoped to that user's own data — there is no cross-user access by id alone.

## Error responses

Errors follow one of two shapes, depending on the failure:

**Domain/auth errors** (`ResourceNotFoundException` → 404, `BadRequestException` → 400, `UnauthorisedException` → 401, and any uncaught exception → 500):

```json
{
  "error": "RESOURCE_NOT_FOUND",
  "message": "Account not found",
  "timestamp": "2026-07-19T10:15:30Z"
}
```

`error` is one of: `RESOURCE_NOT_FOUND`, `BAD_REQUEST`, `UNAUTHORISED`, `INTERNAL_ERROR`. The `INTERNAL_ERROR` message is always the generic `"Something went wrong"` — the real exception is not exposed to the client.

**Bean validation failures** (`@Valid` on a request body, e.g. missing/invalid fields on register or login) return `400` with a flat field → message map instead:

```json
{
  "email": "Must be a valid email address",
  "password": "Password must be at least 8 characters"
}
```

## Pagination

List endpoints that support paging (currently `GET /api/v1/transactions`) accept `page` (0-indexed, default `0`) and `size` (default `20`) query params, and return:

```json
{
  "content": [ /* array of item DTOs */ ],
  "page": 0,
  "size": 20,
  "totalElements": 143,
  "totalPages": 8,
  "last": false
}
```

## Endpoint groups

| Base path | Notes |
|---|---|
| `/api/v1/auth` | `register`, `login` — public |
| `/api/v1/users` | `GET /me`, `DELETE /me` |
| `/api/v1/accounts` | list, create, delete |
| `/api/v1/transactions` | paginated list (filter by `accountId`), get by id, create, update, delete, `POST /import` (multipart CSV upload) |
| `/api/v1/categories` | list |
| `/api/v1/budgets` | list, create, update, delete |
| `/api/v1/dashboard` | summary/aggregate view for the current user |
| `/api/v1/reports` | `GET /spending-by-category?from=&to=`, `GET /income-vs-expense?months=`, `GET /export?format=csv\|pdf&from=&to=` (returns a file download, not JSON) |

## Non-JSON responses

`GET /api/v1/reports/export` is the one endpoint that doesn't return JSON — it streams a `text/csv` or `application/pdf` file with a `Content-Disposition: attachment` header, selected via the `format` query param (`csv` is the default).
