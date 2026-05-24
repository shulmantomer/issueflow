# Custom Instructions Given to the Agent

These are the standing instructions the agent was given when working on IssueFlow. They sit alongside `plan.md` (the architectural plan), `prompts.md` (the actual prompts in order), and `DECISIONS.md` (resolutions to spec ambiguities).

---

## Coding standards

- **Java 21**, Spring Boot 3.4.2. Use records for every request/response DTO; never expose JPA entities through controllers.
- **Layering is non-negotiable**: `Controller → Service (@Transactional, readOnly where appropriate) → Repository`. No cross-layer calls, no controllers reaching into repositories.
- **Validation at the DTO boundary**: `@Valid` + Bean Validation annotations on every request DTO. Custom exceptions for domain rules; let the `GlobalExceptionHandler` produce one uniform error envelope.
- **Audit every state change** via a single `AuditPublisher` bean called inside the same transaction as the mutation.
- **No Lombok on records.** Use Lombok only on entities where the boilerplate cost is real, and prefer explicit code when it's close to a wash.
- **Imports**: never wildcard. Group by JDK / third-party / project.

## Workflow rules

- **Plan first, execute second.** Read the README contract and the requirements PDF end-to-end before writing any code. Lock ambiguous requirements explicitly (see `DECISIONS.md`) before they cause rework.
- **Show every file before writing it.** Walk through the proposed contents, explain what each section does, and wait for explicit approval. No silent edits.
- **One step at a time.** Implement one numbered step from `plan.md`, compile, run the integration tests against a real Postgres, then move on. Never batch multiple steps in a single review pass.
- **Compile + test after each step.** A green build is the unit of progress. Don't stack changes on top of an unverified step.
- **Validate against the README contract, not the implementation's internal model.** When the two disagree, the README wins. Adjust the code; don't rationalize the deviation.
- **Use a real Postgres, not H2.** H2 lies about `BYTEA`, `JSONB`, partial indexes, and case-insensitive collations — every one of those matters here. Integration tests run against the same engine production would.

## Decision style

- **Lock every spec ambiguity explicitly.** If the requirements are vague (logout strategy, concurrency mechanism, what "DEVELOPER in the project" means, escalation cadence, attachment storage location, HTTP status for illegal transitions), pick one resolution, document it in `DECISIONS.md` with a one-line rationale, and stop second-guessing it.
- **Defensible defaults beat clever ones.** Optimistic locking via `@Version` over pessimistic row locks; deny-list logout over stateless expiry; uniform `409` for any "business rule says no" outcome; soft delete via `@SQLRestriction` over manual `WHERE deleted_at IS NULL` everywhere.
- **No half-features.** Either an endpoint exists and is wired end-to-end (controller + service + repository + audit + at least one test), or it doesn't ship in that step.

## What I'm accountable for

Every line of code in this submission was reviewed before it was written. I can defend any decision in `DECISIONS.md`, walk through any service method, and explain the trade-off behind any architectural choice. The agent was a force multiplier on a workflow I drove — plan, review, approve, verify.
