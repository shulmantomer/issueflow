# IssueFlow — Run Guide

Setup, build, run, and test instructions for the IssueFlow backend.

## Prerequisites

- **Java 21 or newer** (developed and tested on Temurin JDK 25)
- **Docker** + Docker Compose (for the PostgreSQL database)
- No Maven install needed — the project ships the Maven wrapper (`mvnw` / `mvnw.cmd`)

## 1. Start the database

```bash
docker compose up -d
```

Starts PostgreSQL on `localhost:5432` (database/user/password all `issueflow`),
matching `src/main/resources/application.yaml`.

## 2. Build

```bash
./mvnw clean package        # macOS/Linux
mvnw.cmd clean package      # Windows
```

## 3. Run the application

```bash
./mvnw spring-boot:run
```

- API base URL: `http://localhost:8080`
- Interactive API docs (Swagger UI): `http://localhost:8080/swagger-ui.html`

On every startup the schema is rebuilt from `schema.sql` and reseeded from `data.sql`.

## 4. Run the tests

The database must be running first — integration tests connect to it:

```bash
docker compose up -d
./mvnw clean test
```

> Run with `clean` — a stale compiled test resource can otherwise shadow the
> active configuration.

The suite covers auth, CRUD, optimistic locking, dependency blocking, CSV
import/export round-trip, audit log filtering, auto-assignment, escalation
rules, comment mention re-evaluation, mention pagination, attachment upload
validation, and the `isOverdue` JSON-key contract.

## Seeded users

Every seeded user has the password **`password123`**:

| Username | Role |
|----------|------|
| admin1, admin2 | ADMIN |
| developer1 – developer4 | DEVELOPER |

Plus 2 projects, 6 tickets, and sample comments / mentions / a dependency.

## Authenticating

1. `POST /auth/login` with `{ "username": "...", "password": "..." }` → `{ "accessToken": "..." }`
2. Send `Authorization: Bearer <accessToken>` on every other request.

`POST /users` (registration) is the only public endpoint.

## Notes

- **JDK 25**: Lombok is pinned to `1.18.46` — Spring Boot 3.4.2's default (1.18.36)
  predates JDK 25 and fails to compile on it.
- The database is ephemeral — `schema.sql` drops and recreates all tables on each
  application/test startup.
- Background jobs: ticket auto-escalation runs every minute; the JWT deny-list
  purge runs hourly.
