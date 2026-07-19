ALTER TABLE deployments
    ADD COLUMN image_tag VARCHAR(255),
    ADD COLUMN duration_ms BIGINT;

ALTER TABLE projects
    ADD COLUMN environment_variables JSONB NOT NULL DEFAULT '{}'::jsonb;