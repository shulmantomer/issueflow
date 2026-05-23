# AI Usage

## Model

This solution was implemented with **Claude Opus 4.7** (`claude-opus-4-7`) via **Claude Code**.

## How AI was used

The project was built with a **plan-then-execute** workflow:

1. **Planning** — Claude read the requirements PDF and the `README.md` API
   contract, explored the provided Spring Boot skeleton, asked clarifying
   questions, and produced a 15-step implementation plan (included as `plan.md`).
2. **Step-by-step execution** — each step was presented for review (every file
   shown and explained *before* being written), approved (often with specific
   adjustments), implemented, compiled, and integration-tested against a
   running PostgreSQL instance before moving on.

Every architectural decision was reviewed and approved before any code was
written. The prompts below are the actual instructions given during the
session, in order.

---

## The prompts

### Planning

> read my file of TDP_issueflow_requirements and tell me how you are going to
> plan to do it. i want to do it in java. so lets plan every step. i want it
> in the highest quality there is

Clarifying questions were answered to lock in: optional password on
`POST /users`, strict one-step status transitions, "any DEVELOPER is an
auto-assignment candidate", and rejecting deletion of a user who owns projects.

### Foundation (Steps 1–3)

> Start with Step 1 only (Foundation). Show me each file before creating it
> and explain what it does. Wait for my approval before moving to Step 2.

> let's go with option 1 for data.sql. it keeps the database seeding separated
> and clean ... since we use soft delete for both projects and tickets
> (section 3.5) we must handle deletes very carefully in our spring boot
> service layer to avoid physical hard deletes from the db. additionally,
> ensure that the ticket status workflow restrictions (moving forward, and no
> updates if DONE) are strictly validated in the service layer, since the DB
> check constraints only validate that the values are legal, not the business
> transition rules.

### Security & Auth (Step 2)

> Group A looks very clean ... Before writing the files, please make one small
> adjustment to UserRepository.java: Add `boolean existsByUsernameIgnoreCase`
> ... we need to prevent duplicate usernames with different casing, since
> mentions are strictly case-insensitive (section 3.6).

> we have a critical requirement issue in SecurityConfig.java: Please add
> `POST /users` to the PUBLIC_PATHS list ... Otherwise, a new anonymous user
> won't be able to register since the entire API is protected by default.
> Also, we are using PostgreSQL via Docker, not H2, so we don't need to
> whitelist H2 paths.

> run the app and verify login works

### Users & Projects (Steps 4–5)

> Please present all Step 4 (Users module) files in one batch. Keep in mind
> these exact requirements: POST /users — if password is empty, generate a
> random one and return it once as generatedPassword. Uniqueness — block
> duplicate usernames using a case-insensitive check, throwing 409 Conflict.
> DELETE /users/:id — reject with 409 if the user is an active project owner.
> Auditing — create a lightweight AuditPublisher interface in common now,
> inject it, and just log the actions for this step to keep things decoupled.

> Please present all Step 5 (Projects module) files in one batch. Use a native
> SQL query in ProjectRepository for GET /projects/deleted to bypass
> Hibernate's soft-delete filter. Secure the /deleted and /:id/restore
> endpoints with @PreAuthorize("hasRole('ADMIN')"). Stub the /workload route
> endpoint for now, as we will fully implement its logic in Step 11.

### Tickets, Comments, Dependencies (Steps 6–8)

> For Step 6 (Tickets module), splitting it into two groups is a smart choice.

> Overriding the plan to drop version from the DTO body is the correct move to
> strictly respect the README contract, while letting JPA's @Version elegantly
> handle the concurrency rule under the hood.

> Mapping comment_mentions as a @ManyToMany collection is the cleanest approach
> to handle automatic diffing on updates.

> The iterative DFS for cycle detection and utilizing @IdClass for the
> composite key mapping are highly professional choices.

### Attachments, CSV, Workload (Steps 9–11)

> The double validation layer using both content-type verification and
> magic-byte sniffing is an excellent security standard.

> Handling the CSV import with per-row transaction isolation to support
> partial success and returning custom row errors is exactly the right
> approach.

> Ensure: the workload query correctly counts only active (non-DONE/non-deleted)
> tickets per developer; the assignment logic implements the tie-break by user
> ID; the AUTO_ASSIGN event is properly logged with AuditActor.SYSTEM.

### Scheduler, Audit, Tests, Docs (Steps 12–15)

> Proceed with Step 12 (Auto-Escalation scheduler). Ensure the @Scheduled job
> is idempotent ... once CRITICAL and still overdue, the overdue flag is set to
> true ... implement the hourly token-denylist purge job.

> PersistentAuditPublisher implements the audit logging interface and correctly
> persists events. The GET /audit-logs endpoint uses JpaSpecificationExecutor
> to allow flexible, optional filtering by entityType, entityId, action, and
> actor.

> For the test suite: use TestRestTemplate to exercise the full HTTP lifecycle,
> including security filters. Ensure each test is isolated (creates its own
> entities to avoid state leakage) even while sharing the Postgres instance.
> Cover the optimistic locking scenario, dependency blocking, and the CSV
> import partial-success scenario end-to-end.

When Testcontainers could not handshake with the local Docker Desktop, the
direction given was to pivot integration tests to the compose-managed
PostgreSQL instance — which also aligns with assignment requirement 4.2.

---

## Files

- `plan.md` — the full 15-step implementation plan that guided the work.
- `run.md` — setup / build / run / test guide.
- `README.md` — the API contract (provided with the assignment).
