ALTER TABLE projects
    ADD COLUMN dockerfile_path VARCHAR(512) DEFAULT 'Dockerfile',
ADD COLUMN context_path VARCHAR(512) DEFAULT '.';