CREATE TABLE projects (
                          id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          uuid VARCHAR(36) NOT NULL UNIQUE,
                          name VARCHAR(100) NOT NULL,
                          description TEXT,
                          github_repo VARCHAR(255),
                          default_branch VARCHAR(50) DEFAULT 'main',
                          owner_id BIGINT NOT NULL,
                          status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP,
                          CONSTRAINT fk_project_owner FOREIGN KEY (owner_id) REFERENCES users(id)
);
