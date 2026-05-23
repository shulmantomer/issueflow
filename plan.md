# IssueFlow — Java Spring Boot Implementation Plan

## Context

The TDP 2026 home assignment asks for a RESTful backend (`IssueFlow`) that manages users, projects, tickets, comments, plus extended features (audit log, dependencies, attachments, CSV import/export, soft delete, @mentions, auto-escalation, auto-assignment by workload). The full requirements are in `TDP2026HW/TDP_issueflow_requirements.pdf`; the API contract is in `TDP2026HW/issueflow-java/README.md`.

The Java skeleton at `TDP2026HW/issueflow-java/` already has:
- Spring Boot **3.4.2** on **Java 21**
- `spring-boot-starter-{web,data-jpa,validation,test}`, PostgreSQL driver, Lombok, Apache Commons CSV 1.10.0, H2 (test scope)
- `compose.yml` for local PostgreSQL (db=`issueflow`, user/pass=`issueflow/issueflow`, port 5432)
- `application.yaml` with `ddl-auto: update`, `sql.init.mode: always`, 10MB multipart limit
- A throwaway `task` table in `schema.sql`/`data.sql` that must be replaced
- An empty `IssueFlowApplication` class — **no domain code yet**

What is **missing** and must be added: Spring Security, a JWT library, scheduling, and the entire domain (entities, repositories, services, controllers, DTOs, exception handling, security config, seed data, tests).

The goal: a clean, layered, production-quality implementation that passes the README API contract end-to-end.

---

## High-Level Architecture

Standard layered Spring Boot architecture under base package `com.att.tdp.issueflow`:

```
com.att.tdp.issueflow
├── IssueFlowApplication.java        # @SpringBootApplication, @EnableScheduling, @EnableJpaAuditing
├── config/                          # SecurityConfig, OpenApiConfig, JacksonConfig
├── security/                        # JwtService, JwtAuthFilter, JwtAuthEntryPoint, TokenDenyList, AuthenticatedUser
├── exception/                       # GlobalExceptionHandler, custom exceptions (NotFound, Conflict, Forbidden, BadRequest)
├── common/                          # BaseAuditEntity, enums (Role, Status, Priority, Type, Actor, Action, EntityType)
├── audit/                           # AuditLog entity + repository + service + controller; AuditPublisher (used by other services)
├── user/                            # User entity, repo, service, controller, DTOs
├── auth/                            # AuthController, AuthService, login/logout/me DTOs
├── project/                         # Project entity, repo, service, controller, DTOs
├── ticket/                          # Ticket entity, repo, service, controller, DTOs, status-transition validator
├── ticket/dependency/               # TicketDependency entity + endpoints
├── ticket/attachment/               # Attachment entity + endpoints (DB-stored bytea, content-type whitelist)
├── ticket/csv/                      # CsvExportService, CsvImportService (Apache Commons CSV)
├── comment/                         # Comment entity, repo, service, controller, DTOs
├── mention/                         # Mention entity, MentionExtractor (@username regex), MentionService, MentionController
└── scheduling/                      # EscalationScheduler (every minute @Scheduled)
```

**Cross-cutting decisions:**
- **DTO boundary**: never expose JPA entities through controllers — every request/response is a record-based DTO (Java 21 records). MapStruct is overkill for this scope; hand-write tiny mappers next to each DTO.
- **Validation**: `@Valid` + Bean Validation annotations on DTOs; enum values validated by Jackson deserialization plus custom `@ValidEnum` where needed.
- **Auditing**: a single `AuditPublisher` bean injected into every service that performs state changes. Each mutation calls it after the DB commit (use Spring's `TransactionSynchronizationManager` or simply call inside the same `@Transactional`).
- **Concurrent edits** on tickets/comments: JPA `@Version` (optimistic locking). Clients must echo back `version` on PATCH; conflict → 409. Simpler and safer than row locks.
- **Soft delete**: a `deletedAt TIMESTAMP NULL` column on `Project` and `Ticket`. Standard repository methods filter by `deletedAt IS NULL` via Hibernate `@SQLRestriction` (Hibernate 6); dedicated repo methods use native queries when listing/restoring deleted rows.
- **IDs**: `BIGINT` `IDENTITY` (Postgres) — keep numeric IDs as the README shows.
- **Timestamps**: `Instant` (UTC) everywhere; `created_at`/`updated_at` via `@CreatedDate`/`@LastModifiedDate` (`@EnableJpaAuditing`).

---

## Step-by-Step Implementation Plan

### Step 1 — pom.xml additions
Add to `TDP2026HW/issueflow-java/pom.xml`:
- `spring-boot-starter-security`
- `io.jsonwebtoken:jjwt-api:0.12.6` + `jjwt-impl` (runtime) + `jjwt-jackson` (runtime) — battle-tested, simple
- `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0` — auto-generates Swagger UI at `/swagger-ui.html` for graders to poke at
- `org.testcontainers:postgresql:1.20.4` + `testcontainers:junit-jupiter` (test scope) — integration tests against real Postgres, not H2 (H2 doesn't match Postgres semantics for `BYTEA`, `JSONB`, partial indexes, etc.)

### Step 2 — Replace `schema.sql` and `data.sql`
**Delete** the `task` table. Write a complete schema covering all entities. Representative tables (full DDL goes in the file):

```
users(id, username UNIQUE, email UNIQUE, full_name, role, password_hash, created_at, updated_at)
projects(id, name, description, owner_id FK users, deleted_at NULL, version, created_at, updated_at)
tickets(id, title, description, status, priority, type, project_id FK, assignee_id FK NULL,
        due_date NULL, is_overdue BOOL, deleted_at NULL, version, created_at, updated_at)
ticket_dependencies(ticket_id, blocked_by_id, PRIMARY KEY(ticket_id, blocked_by_id))
comments(id, ticket_id FK, author_id FK, content, version, created_at, updated_at)
comment_mentions(comment_id, user_id, PRIMARY KEY(comment_id, user_id))
attachments(id, ticket_id FK, filename, content_type, size_bytes, data BYTEA, uploaded_by FK, created_at)
audit_logs(id, action, entity_type, entity_id, performed_by NULL, actor, payload JSONB, timestamp)
token_denylist(jti PRIMARY KEY, expires_at)  -- for /auth/logout
```
Plus indexes: `tickets(project_id)`, `tickets(assignee_id, status)` (for workload), `audit_logs(entity_type, entity_id)`, `comment_mentions(user_id)`.

Switch `spring.jpa.hibernate.ddl-auto` from `update` to `validate` so the SQL schema is authoritative; keep `sql.init.mode: always`. Seed `data.sql` with 2 admins, 4 developers (BCrypt-hashed `password123`), 2 projects, ~6 tickets, a couple of comments.

### Step 3 — Common building blocks
- `common/enums/`: `Role`, `TicketStatus`, `TicketPriority`, `TicketType`, `AuditAction`, `AuditEntityType`, `AuditActor`.
- `common/BaseAuditEntity`: `@MappedSuperclass` with `createdAt`/`updatedAt` and `@EntityListeners(AuditingEntityListener.class)`.
- `exception/`: typed exceptions (`NotFoundException`, `ConflictException`, `ForbiddenException`, `BadRequestException`) and a `@RestControllerAdvice` `GlobalExceptionHandler` that maps everything to a uniform error body: `{ "timestamp", "status", "error", "message", "path", "fieldErrors": [...] }`. Handles `MethodArgumentNotValidException`, `HttpMessageNotReadableException`, `DataIntegrityViolationException`, `OptimisticLockException`, `MaxUploadSizeExceededException`.

### Step 4 — Security & Auth (`auth/`, `security/`, `config/SecurityConfig`)
- `User` entity holds `password_hash` (BCrypt). `POST /users` accepts an optional `password`; if absent, generate a random 16-char password and return it once as `generatedPassword` in the response (so a graders can immediately log in as the new user).
- `JwtService`: signs HS256 with a key from `app.jwt.secret`, claims = `sub=username`, `uid`, `role`, `jti`, `iat`, `exp` (default 1h via `app.jwt.expires-in`).
- `JwtAuthFilter` extends `OncePerRequestFilter`: extracts `Bearer` token, validates signature/expiry, checks `TokenDenyList`, populates `SecurityContextHolder` with an `AuthenticatedUser` principal.
- `TokenDenyList`: backed by the `token_denylist` table (jti + expiresAt). A `@Scheduled` job purges expired rows hourly.
- `SecurityConfig`: stateless, CSRF off, `/auth/login` + `/swagger-ui/**` + `/v3/api-docs/**` permitted; everything else requires JWT. `@PreAuthorize("hasRole('ADMIN')")` guards admin-only endpoints (soft-deleted listings, restore).
- `AuthController`:
  - `POST /auth/login` → returns `{ accessToken, tokenType:"Bearer", expiresIn }`.
  - `POST /auth/logout` → adds current jti to deny-list.
  - `GET /auth/me` → returns the current `UserResponse`.

### Step 5 — Users (`user/`)
- Entity, `UserRepository extends JpaRepository<User, Long>`, `UserService`, `UserController`.
- Endpoints exactly as in README (incl. the unusual `POST /users/update/:userId`).
- Validation: `@NotBlank username`, `@Email email`, role from enum. Username/email uniqueness → 409 on `DataIntegrityViolation` mapped by the global handler.
- Delete: hard delete (README spec). If user owns ≥1 project → 409. Otherwise, `assignee_id` on their tickets is set to null via `ON DELETE SET NULL`; same for `author_id` on comments (the comment row stays as a historical record).

### Step 6 — Projects (`project/`)
- CRUD + soft delete. `DELETE /projects/:id` sets `deletedAt = now()`. `GET /projects/deleted` (ADMIN) lists soft-deleted rows. `POST /projects/:id/restore` (ADMIN) clears `deletedAt`.
- `GET /projects/:projectId/workload` (see Step 11).
- All mutations call `AuditPublisher.publish(CREATE/UPDATE/DELETE, PROJECT, id, USER, actorId)`.

### Step 7 — Tickets (`ticket/`)
The richest module. Implementation notes:
- **Status transition validator**: `TicketStatusTransitions.assertAllowed(current, next)` enforces strict **one-step forward only** — TODO→IN_PROGRESS, IN_PROGRESS→IN_REVIEW, IN_REVIEW→DONE. Any other (skip, backward, same) → 409.
- **Cannot update once DONE**: in `update()`, throw 409 if `currentStatus == DONE`.
- **Cannot transition to DONE if blocked**: before allowing a transition to DONE, query `ticket_dependencies` and confirm all blockers are DONE; otherwise 409 with the blocker IDs in the response body.
- **Optimistic locking**: `@Version Long version` on Ticket; PATCH body must include `version` field — surface as 409 on `OptimisticLockException`.
- **Soft delete**, **CSV export/import**, **dependencies**, **attachments** as separate sub-features below.
- **Auto-assignment on create when assigneeId absent**: see Step 11.
- **Manual priority change resets `is_overdue`**: the service compares the incoming `priority` to the stored value; if changed by user, set `is_overdue = false`.

### Step 8 — Comments (`comment/`) + Mentions (`mention/`)
- `CommentRepository`. Endpoints: list per ticket, add, update (with `@Version` for concurrent-edit prevention → 409 on conflict), delete.
- `MentionExtractor`: regex `@([A-Za-z0-9_]+)` (case-insensitive lookup) → resolves to `User` rows. Unknown usernames are silently dropped (not an error — they're just not real users).
- On comment create/update, recompute the mention set: insert new, delete removed (`comment_mentions` is the join table).
- Response DTO includes `mentionedUsers: [{ id, username, fullName }]`.
- `GET /users/:userId/mentions?page=&pageSize=` — paginated, newest first, joined with comment info.

### Step 9 — Ticket Dependencies (`ticket/dependency/`)
- Endpoints `POST/GET/DELETE /tickets/:ticketId/dependencies[/:blockerId]`.
- Validation: both tickets exist, same project, no self-dependency, no cycle (DFS check before insert).
- The transition-to-DONE rule (Step 7) consumes this table.

### Step 10 — Attachments (`ticket/attachment/`)
- `POST /tickets/:ticketId/attachments` — multipart `file`. Reject if size > 10MB (already enforced by `spring.servlet.multipart.max-file-size`; mapped to a clean 413). Whitelist `image/png`, `image/jpeg`, `application/pdf`, `text/plain` — both by `MultipartFile.getContentType()` **and** by sniffing magic bytes (`Apache Tika` is overkill; just check first bytes for png/jpeg/pdf and use UTF-8 decodability for text/plain). Store bytes in `attachments.data BYTEA`.
- `DELETE /tickets/:ticketId/attachments/:attachmentId`.

### Step 11 — CSV export/import (`ticket/csv/`)
- `GET /tickets/export?projectId=` → streams `text/csv` using `CSVPrinter` (`CSVFormat.DEFAULT.withHeader(...)`), columns: `id,title,description,status,priority,type,assigneeId`. Quoting handled by Commons CSV.
- `POST /tickets/import` → multipart `file` + form field `projectId`. Stream lines with `CSVParser`. For each row: validate, attempt to create. Collect failures into `errors: [{ row, message }]`. Return `{ created, failed, errors }`. The whole import runs in **one transaction per row** (not a single big tx) so partial success works.

### Step 12 — Workload + Auto-Assignment (`ticket/` service)
- `GET /projects/:projectId/workload`: returns one row per **every** DEVELOPER in the system (no project membership concept). Each row's `openTicketCount` is the count of non-DONE, non-deleted tickets that user is assigned in **this specific project**. Sorted ascending by count, then by `users.id` ascending.
- On ticket create without `assigneeId`: same query, pick lowest count, tiebreak by `users.id` ascending (registration order). If empty, leave `assigneeId = null`. Emit an `AUTO_ASSIGN` audit row with `actor = SYSTEM`.

### Step 13 — Auto-Escalation (`scheduling/EscalationScheduler`)
- `@Scheduled(cron = "0 * * * * *")` (every minute).
- For each non-deleted ticket with `due_date < now()`, `status != DONE`:
  - If `priority < CRITICAL`: promote one level, emit `AUTO_ESCALATE` audit.
  - If already `CRITICAL` and overdue: set `is_overdue = true` (idempotent).
- Wrapped in a single transaction; uses a `SELECT ... FOR UPDATE SKIP LOCKED` pattern if we want to be safe under multi-instance — for this assignment a single instance is fine, so a plain query is enough.

### Step 14 — Audit Log (`audit/`)
- `AuditPublisher.publish(action, entityType, entityId, actor, performedBy, payload)` — called by every mutating service method.
- `GET /audit-logs` with optional `?entityType=&entityId=&action=&actor=` — Spring Data JPA `Specification` to compose the WHERE clause.

### Step 15 — Testing
Three layers:
1. **Unit tests** (JUnit 5 + Mockito) — service-layer logic: status transitions, mention extraction, escalation rules, auto-assignment tiebreak, CSV row parsing.
2. **Integration tests** (`@SpringBootTest` + Testcontainers Postgres) — full HTTP round-trips with `MockMvc`/`TestRestTemplate`: auth, CRUD per entity, optimistic-lock conflict, soft-delete + restore, CSV roundtrip, dependency-blocks-DONE, scheduler invocation via a manual trigger bean.
3. **Smoke**: keep the existing `contextLoads()` test.

Aim for **>80%** line coverage on service classes, every controller endpoint has at least one happy-path + one validation-failure test.

### Step 16 — Documentation (`run.md`, `prompts.md`)
- `run.md`: prerequisites (Docker, Java 21), `docker compose up -d`, `./mvnw clean package`, `./mvnw spring-boot:run`, `./mvnw test`. Seed credentials, sample `curl` commands hitting each endpoint, Swagger UI URL.
- `prompts.md`: model used (Claude Opus 4.7), key prompts, plan link, instruction files.

---

## Critical Files to Modify / Create

These are the load-bearing files; the rest follow the layered template above:

- `TDP2026HW/issueflow-java/pom.xml` — add Security, JJWT, springdoc, Testcontainers.
- `TDP2026HW/issueflow-java/src/main/resources/schema.sql` — full replacement.
- `TDP2026HW/issueflow-java/src/main/resources/data.sql` — seed users/projects/tickets.
- `TDP2026HW/issueflow-java/src/main/resources/application.yaml` — add `app.jwt.*` props, switch `ddl-auto` to `validate`.
- `TDP2026HW/issueflow-java/src/main/java/com/att/tdp/issueflow/IssueFlowApplication.java` — add `@EnableScheduling`, `@EnableJpaAuditing`.
- `config/SecurityConfig.java` — single source of truth for the security chain.
- `ticket/TicketService.java` — the busiest service: status transitions, dependency-blocks-DONE, optimistic locking, auto-assignment, soft delete, escalation reset on manual priority change.
- `scheduling/EscalationScheduler.java` — the only `@Scheduled` job (plus deny-list purge).
- `audit/AuditPublisher.java` — used everywhere, must be cheap and reliable.
- `exception/GlobalExceptionHandler.java` — uniform error envelope.

---

## Decisions Locked In

1. **Passwords**: `POST /users` accepts an **optional** `password`. If present, BCrypt-hash it. If absent, generate a random 16-char password and return it **once** in the response body (`generatedPassword` field). Login uses BCrypt verification.
2. **Status transitions**: **Strict one-step** forward only. `TicketStatusTransitions` allows TODO→IN_PROGRESS, IN_PROGRESS→IN_REVIEW, IN_REVIEW→DONE only. Any other transition (including TODO→DONE, IN_REVIEW→TODO, etc.) → 409 with a clear message.
3. **Workload candidates**: **Any DEVELOPER** in the system is a candidate for auto-assignment and appears in `/projects/:projectId/workload`. `openTicketCount` is the count of that user's non-DONE, non-deleted tickets **in the given project**. Tiebreak by `users.id` ascending.
4. **DELETE /users**: If the user owns ≥1 project → reject with **409** (`"Cannot delete user: owns N project(s). Reassign first."`). Otherwise, null out `assignee_id` on their tickets (foreign key `ON DELETE SET NULL`) and delete the user. Comments stay (author_id is preserved as a historical record; consider keeping the FK with `ON DELETE SET NULL` too — comments without an author render as "deleted user").

---

## Verification

End-to-end manual verification once implementation is done:

1. `cd TDP2026HW/issueflow-java && docker compose up -d` — Postgres up on 5432.
2. `./mvnw clean package` — builds cleanly, no warnings beyond Lombok.
3. `./mvnw spring-boot:run` — app starts on 8080, schema validates.
4. Open `http://localhost:8080/swagger-ui.html` — every endpoint visible.
5. `curl -X POST localhost:8080/auth/login -d '{"username":"admin","password":"password123"}' -H 'Content-Type: application/json'` → JWT.
6. Walk the README contract endpoint-by-endpoint with `curl` (or Swagger): create project → create ticket → add comment with `@developer1` → list mentions → upload PNG attachment → add dependency → try moving ticket to DONE (blocked) → resolve blocker → DONE works → soft-delete project → restore as admin → export tickets CSV → import CSV → check audit log.
7. Force overdue: insert a ticket with `due_date = now() - 1 day` and watch the scheduler bump priority within one minute; audit log shows `AUTO_ESCALATE` rows.
8. `./mvnw test` — all tests green, coverage report (Jacoco optional) shows >80% on service layer.
