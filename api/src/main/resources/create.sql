CREATE TABLE IF NOT EXISTS user_github
(
    id     BIGINT PRIMARY KEY,
    login  VARCHAR(39) UNIQUE NOT NULL,
    name   VARCHAR(256),
    avatar VARCHAR(2000)      NOT NULL
);

CREATE TABLE IF NOT EXISTS user_internal
(
    id              BIGSERIAL PRIMARY KEY,
    gh_id           BIGINT UNIQUE NOT NULL,
    gh_access_token VARCHAR(256),

    FOREIGN KEY (gh_id) REFERENCES user_github (id)
);

CREATE TABLE IF NOT EXISTS refresh_token
(
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT                   NOT NULL,
    token           VARCHAR(256) UNIQUE      NOT NULL,
    expiration_date TIMESTAMP WITH TIME ZONE NOT NULL,

    FOREIGN KEY (user_id) REFERENCES user_internal (id)
);

CREATE TABLE IF NOT EXISTS project
(
    id                  BIGSERIAL PRIMARY KEY,
    gh_repo_id          BIGINT UNIQUE NOT NULL,
    creator_id          BIGINT        NOT NULL,
    gh_repo_name        VARCHAR(256)  NOT NULL,
    gh_repo_owner       VARCHAR(39)   NOT NULL,
    gh_repo_description TEXT          NOT NULL,

    FOREIGN KEY (creator_id) REFERENCES user_internal (id)
);

CREATE TABLE IF NOT EXISTS project_admin
(
    project_id BIGSERIAL NOT NULL,
    admin_id   BIGSERIAL NOT NULL,

    FOREIGN KEY (project_id) REFERENCES project (id),
    FOREIGN KEY (admin_id) REFERENCES user_internal (id),
    CONSTRAINT project_admin_pk PRIMARY KEY (project_id, admin_id)
);

CREATE TABLE IF NOT EXISTS commit
(
    id    BIGSERIAL PRIMARY KEY,
    owner VARCHAR(39)     NOT NULL,
    repo  VARCHAR(256)    NOT NULL,
    sha   CHAR(40) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS pull
(
    id                  BIGSERIAL PRIMARY KEY,
    project_id          BIGINT                   NOT NULL,
    number              BIGINT                   NOT NULL,
    title               VARCHAR(128)             NOT NULL,
    open                BOOLEAN                  NOT NULL,

    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,

    head_last_commit_id BIGINT                   NOT NULL,
    head_branch         VARCHAR(256)             NOT NULL,

    base_last_commit_id BIGINT                   NOT NULL,
    base_branch         VARCHAR(256)             NOT NULL,

    author_gh_id        BIGINT                   NOT NULL,

    FOREIGN KEY (project_id) REFERENCES project (id),
    FOREIGN KEY (head_last_commit_id) REFERENCES commit (id),
    FOREIGN KEY (base_last_commit_id) REFERENCES commit (id),
    FOREIGN KEY (author_gh_id) REFERENCES user_github (id)
);

CREATE TABLE IF NOT EXISTS clone
(
    id               BIGSERIAL PRIMARY KEY,
    target_commit_id BIGINT       NOT NULL,
    target_file      VARCHAR(256) NOT NULL,
    target_from_line INT          NOT NULL,
    target_to_line   INT          NOT NULL,
    source_commit_id BIGINT       NOT NULL,
    source_file      VARCHAR(256) NOT NULL,
    source_from_line INT          NOT NULL,
    source_to_line   INT          NOT NULL,
    suppressed       BOOLEAN      NOT NULL DEFAULT FALSE,

    FOREIGN KEY (target_commit_id) REFERENCES commit (id),
    FOREIGN KEY (source_commit_id) REFERENCES commit (id)
);
