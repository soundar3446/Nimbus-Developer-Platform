CREATE TABLE github_integrations (
                                     id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                     user_id BIGINT NOT NULL UNIQUE,
                                     github_id VARCHAR(100) UNIQUE NOT NULL,
                                     github_username VARCHAR(100) NOT NULL,
                                     github_access_token VARCHAR(255) NOT NULL,
                                     github_avatar VARCHAR(255),
                                     created_at TIMESTAMP NOT NULL,
                                     updated_at TIMESTAMP NOT NULL,
                                     CONSTRAINT fk_github_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);