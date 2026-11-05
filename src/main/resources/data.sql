-- IssueFlow seed data. Runs after schema.sql on every startup (sql.init.mode=always).
-- Password for every seeded user is: password123  (BCrypt cost 10)

INSERT INTO users (id, username, email, full_name, role, password_hash) VALUES
  (1, 'admin1',     'admin1@issueflow.test',     'Alice Admin',  'ADMIN',     '$2a$10$BEd6x7unEAPh3q5YCJseuezzAMBN.9q7bq70dDk4QCfcwOG.9pNlO'),
  (2, 'admin2',     'admin2@issueflow.test',     'Bruno Admin',  'ADMIN',     '$2a$10$BEd6x7unEAPh3q5YCJseuezzAMBN.9q7bq70dDk4QCfcwOG.9pNlO'),
  (3, 'developer1', 'developer1@issueflow.test', 'Dana Dev',     'DEVELOPER', '$2a$10$BEd6x7unEAPh3q5YCJseuezzAMBN.9q7bq70dDk4QCfcwOG.9pNlO'),
  (4, 'developer2', 'developer2@issueflow.test', 'Diego Dev',    'DEVELOPER', '$2a$10$BEd6x7unEAPh3q5YCJseuezzAMBN.9q7bq70dDk4QCfcwOG.9pNlO'),
  (5, 'developer3', 'developer3@issueflow.test', 'Devi Dev',     'DEVELOPER', '$2a$10$BEd6x7unEAPh3q5YCJseuezzAMBN.9q7bq70dDk4QCfcwOG.9pNlO'),
  (6, 'developer4', 'developer4@issueflow.test', 'Dmitri Dev',   'DEVELOPER', '$2a$10$BEd6x7unEAPh3q5YCJseuezzAMBN.9q7bq70dDk4QCfcwOG.9pNlO');
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));

INSERT INTO projects (id, name, description, owner_id) VALUES
  (1, 'Phoenix',  'Customer-facing web rewrite',  1),
  (2, 'Atlas',    'Internal analytics platform',  2);
SELECT setval('projects_id_seq', (SELECT MAX(id) FROM projects));

INSERT INTO tickets (id, title, description, status, priority, type, project_id, assignee_id, due_date, is_overdue) VALUES
  (1, 'Fix login bug',           'Users see 500 on login with empty password.', 'TODO',        'HIGH',     'BUG',       1, 3, '2026-06-01T00:00:00Z', false),
  (2, 'Add dark mode',           'Toggle in settings, persist per user.',        'IN_PROGRESS', 'MEDIUM',   'FEATURE',   1, 4, '2026-07-15T00:00:00Z', false),
  (3, 'Upgrade Postgres driver', 'Move to 42.7.x, verify SSL options.',          'TODO',        'LOW',      'TECHNICAL', 1, 5, NULL,                   false),
  (4, 'Dashboard charts',        'Render P50/P95 latency over 7 days.',          'IN_REVIEW',   'HIGH',     'FEATURE',   2, 3, '2026-05-25T00:00:00Z', false),
  (5, 'CSV export crash',        'NPE on tickets with null assignee.',           'TODO',        'CRITICAL', 'BUG',       2, 6, '2026-04-01T00:00:00Z', false),
  (6, 'Old overdue ticket',      'Seeded overdue to exercise escalation.',       'TODO',        'LOW',      'TECHNICAL', 1, 4, '2026-01-01T00:00:00Z', false);
SELECT setval('tickets_id_seq', (SELECT MAX(id) FROM tickets));

INSERT INTO ticket_dependencies (ticket_id, blocked_by_id) VALUES
  (2, 1);

INSERT INTO comments (id, ticket_id, author_id, content) VALUES
  (1, 1, 3, 'Reproduced locally. @developer2 can you take a look at the validator?'),
  (2, 1, 4, 'On it — I think it''s in LoginController#authenticate.');
SELECT setval('comments_id_seq', (SELECT MAX(id) FROM comments));

INSERT INTO comment_mentions (comment_id, user_id) VALUES
  (1, 4);
