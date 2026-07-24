ALTER TABLE projects
    ADD COLUMN subdomain VARCHAR unique ,
    ADD COLUMN custom_domain VARCHAR unique,
    ADD COLUMN custom_domain_verified BOOLEAN;