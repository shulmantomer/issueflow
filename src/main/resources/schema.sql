-- IssueFlow schema — authoritative. JPA runs in ddl-auto=validate against this.

DROP TABLE IF EXISTS token_denylist           CASCADE;
DROP TABLE IF EXISTS audit_logs               CASCADE;
DROP TABLE IF EXISTS attachments              CASCADE;
DROP TABLE IF EXISTS comment_mentions         CASCADE;
DROP TABLE IF EXISTS comments                 CASCADE;
DROP TABLE IF EXISTS ticket_dependencies      CASCADE;
DROP TABLE IF EXISTS tickets                  CASCADE;
DROP TABLE IF EXISTS projects                 CASCADE;
DROP TABLE IF EXISTS users                    CASCADE;
DROP TABLE IF EXISTS task                     CASCADE;

CREATE TABLE users (
    id            BIGSERIAL    PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    full_name     VARCHAR(255) NOT NULL,
    role          VARCHAR(16)  NOT NULL CHECK (role IN ('ADMIN', 'DEVELOPER')),
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE projects (
    id           BIGSERIAL    PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    description  TEXT,
    owner_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    deleted_at   TIMESTAMPTZ  NULL,
    version      BIGINT       NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_projects_deleted_at ON projects(deleted_at);

CREATE TABLE tickets (
    id           BIGSERIAL    PRIMARY KEY,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    status       VARCHAR(16)  NOT NULL CHECK (status   IN ('TODO','IN_PROGRESS','IN_REVIEW','DONE')),
    priority     VARCHAR(16)  NOT NULL CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    type         VARCHAR(16)  NOT NULL CHECK (type     IN ('BUG','FEATURE','TECHNICAL')),
    project_id   BIGINT       NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    assignee_id  BIGINT       NULL     REFERENCES users(id)    ON DELETE SET NULL,
    due_date     TIMESTAMPTZ  NULL,
    is_overdue   BOOLEAN      NOT NULL DEFAULT false,
    deleted_at   TIMESTAMPTZ  NULL,
    version      BIGINT       NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_tickets_project           ON tickets(project_id);
CREATE INDEX idx_tickets_assignee_status   ON tickets(assignee_id, status);
CREATE INDEX idx_tickets_deleted_at        ON tickets(deleted_at);
CREATE INDEX idx_tickets_due_status        ON tickets(due_date, status) WHERE due_date IS NOT NULL;

CREATE TABLE ticket_dependencies (
    ticket_id     BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    blocked_by_id BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (ticket_id, blocked_by_id),
    CHECK (ticket_id <> blocked_by_id)
);

CREATE TABLE comments (
    id         BIGSERIAL   PRIMARY KEY,
    ticket_id  BIGINT      NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    author_id  BIGINT      NULL     REFERENCES users(id)   ON DELETE SET NULL,
    content    TEXT        NOT NULL,
    version    BIGINT      NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_comments_ticket ON comments(ticket_id);

CREATE TABLE comment_mentions (
    comment_id BIGINT NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    PRIMARY KEY (comment_id, user_id)
);
CREATE INDEX idx_comment_mentions_user ON comment_mentions(user_id);

CREATE TABLE attachments (
    id           BIGSERIAL    PRIMARY KEY,
    ticket_id    BIGINT       NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    filename     VARCHAR(255) NOT NULL,
    content_type VARCHAR(127) NOT NULL,
    size_bytes   BIGINT       NOT NULL,
    data         BYTEA        NOT NULL,
    uploaded_by  BIGINT       NULL REFERENCES users(id) ON DELETE SET NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_attachments_ticket ON attachments(ticket_id);

CREATE TABLE audit_logs (
    id           BIGSERIAL    PRIMARY KEY,
    action       VARCHAR(32)  NOT NULL,
    entity_type  VARCHAR(32)  NOT NULL,
    entity_id    BIGINT       NULL,
    performed_by BIGINT       NULL REFERENCES users(id) ON DELETE SET NULL,
    actor        VARCHAR(16)  NOT NULL CHECK (actor IN ('USER','SYSTEM')),
    payload      JSONB        NULL,
    timestamp    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_actor  ON audit_logs(actor);

CREATE TABLE token_denylist (
    jti        VARCHAR(64)  PRIMARY KEY,
    expires_at TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_token_denylist_expires ON token_denylist(expires_at);
