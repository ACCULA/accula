CREATE TYPE user_role_enum AS ENUM ('USER', 'ADMIN', 'ROOT');

ALTER TABLE user_
    ADD COLUMN role user_role_enum NOT NULL DEFAULT 'USER';

UPDATE user_
SET role = 'ROOT'
WHERE github_id IN (10428179, 15687094, 28392375, 665821);
