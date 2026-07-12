CREATE TABLE deployments (
                             id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                             project_id BIGINT NOT NULL,
                             status VARCHAR(50) NOT NULL, -- e.g., QUEUED, CLONING, DETECTING, BUILDING, DEPLOYED, FAILED
                             git_commit_hash VARCHAR(100),
                             docker_image_tag VARCHAR(255),
                             log_file_path VARCHAR(512),
                             created_at TIMESTAMP NOT NULL,
                             updated_at TIMESTAMP NOT NULL,
                             CONSTRAINT fk_deployment_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);