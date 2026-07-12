ALTER TABLE users
    ADD COLUMN user_role VARCHAR(20);

UPDATE users
SET user_role='USER';