CREATE TABLE IF NOT EXISTS users
(
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(256),

    github_id           BIGINT UNIQUE      NOT NULL,
    github_login        VARCHAR(39) UNIQUE NOT NULL,
    github_access_token VARCHAR(256)
);

CREATE TABLE IF NOT EXISTS refresh_token
(
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT                   NOT NULL,
    token           VARCHAR(256) UNIQUE      NOT NULL,
    expiration_date TIMESTAMP WITH TIME ZONE NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS project
(
    id                   BIGSERIAL PRIMARY KEY,
    creator_id           BIGINT        NOT NULL,
    repo_url             VARCHAR(2000) NOT NULL,
    repo_name            VARCHAR(256)  NOT NULL,
    repo_description     TEXT          NOT NULL,
    repo_open_pull_count INT           NOT NULL,
    repo_owner           VARCHAR(39)   NOT NULL,
    repo_owner_avatar    VARCHAR(2000) NOT NULL,
    admins               BIGINT[]      NOT NULL,

    FOREIGN KEY (creator_id) REFERENCES users (id)
);
