ALTER TABLE deployments
    ADD COLUMN container_id VARCHAR(64),
    ADD COLUMN container_name VARCHAR(255),
    ADD COLUMN host_port INTEGER,
    ADD COLUMN application_url VARCHAR(512);