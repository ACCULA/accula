CREATE TABLE IF NOT EXISTS projects
(
    id               BIGSERIAL PRIMARY KEY,
    url              VARCHAR(512)            NOT NULL,
    repository_owner VARCHAR(128)            NOT NULL,
    name             VARCHAR(256)            NOT NULL,
    description      VARCHAR(2048)           NOT NULL,
    avatar           VARCHAR(256)            NOT NULL,
    user_id          BIGINT                  NOT NULL,
    open_pull_count  INT                     NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users (id)
);

-- CREATE TABLE IF NOT EXISTS project_admin
-- (
--     user_id         BIGINT                  NOT NULL,
--     project_id      BIGINT                  NOT NULL,
--
--     FOREIGN KEY (user_id)    REFERENCES users (id)    ON UPDATE CASCADE ON DELETE CASCADE,
--     FOREIGN KEY (project_id) REFERENCES projects (id) ON UPDATE CASCADE ON DELETE CASCADE
-- );