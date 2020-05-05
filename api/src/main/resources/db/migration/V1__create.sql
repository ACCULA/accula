CREATE TABLE IF NOT EXISTS users
(
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(64)        NOT NULL,

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
