CREATE TABLE IF NOT EXISTS user_github
(
    id     BIGINT PRIMARY KEY,
    login  VARCHAR(39) UNIQUE    NOT NULL,
    name   VARCHAR(256),
    avatar VARCHAR(128)          NOT NULL,
    is_org BOOLEAN DEFAULT FALSE NOT NULL
);

CREATE TABLE IF NOT EXISTS user_
(
    id                  BIGSERIAL PRIMARY KEY,
    github_id           BIGINT UNIQUE NOT NULL,
    github_access_token VARCHAR(256)  NOT NULL,

    FOREIGN KEY (github_id) REFERENCES user_github (id)
);

CREATE TABLE IF NOT EXISTS refresh_token
(
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT                   NOT NULL,
    token           VARCHAR(256) UNIQUE      NOT NULL,
    expiration_date TIMESTAMP WITH TIME ZONE NOT NULL,

    FOREIGN KEY (user_id) REFERENCES user_ (id)
);

CREATE TABLE IF NOT EXISTS repo_github
(
    id             BIGINT PRIMARY KEY,
    name           VARCHAR(256) NOT NULL,
    owner_id       BIGINT       NOT NULL,
    description    TEXT         NOT NULL,
    forked_from_id BIGINT DEFAULT NULL,

    FOREIGN KEY (owner_id) REFERENCES user_github (id),
    FOREIGN KEY (forked_from_id) REFERENCES repo_github (id)
);

CREATE TYPE project_state_enum AS ENUM ('CREATING', 'CREATED');

CREATE TABLE IF NOT EXISTS project
(
    id             BIGSERIAL PRIMARY KEY,
    state          project_state_enum NOT NULL DEFAULT 'CREATING',
    github_repo_id BIGINT UNIQUE      NOT NULL,
    creator_id     BIGINT             NOT NULL,

    FOREIGN KEY (github_repo_id) REFERENCES repo_github (id),
    FOREIGN KEY (creator_id) REFERENCES user_ (id)
);

CREATE TABLE IF NOT EXISTS project_admin
(
    project_id BIGSERIAL NOT NULL,
    admin_id   BIGSERIAL NOT NULL,

    FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE,
    FOREIGN KEY (admin_id) REFERENCES user_ (id),
    CONSTRAINT project_admin_pk PRIMARY KEY (project_id, admin_id)
);

CREATE TABLE IF NOT EXISTS project_conf
(
    project_id                BIGINT PRIMARY KEY,
    clone_min_token_count     INT    NOT NULL,
    file_min_similarity_index INT    NOT NULL,
    excluded_files            TEXT[] NOT NULL,

    FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS commit
(
    sha          CHAR(40) PRIMARY KEY,
    is_merge     BOOLEAN                  NOT NULL,
    author_name  VARCHAR(256)             NOT NULL,
    author_email VARCHAR(256)             NOT NULL,
    date         TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS snapshot
(
    sha     CHAR(40)     NOT NULL,
    repo_id BIGINT       NOT NULL,
    branch  VARCHAR(256) NOT NULL,

    FOREIGN KEY (sha) REFERENCES commit (sha),
    FOREIGN KEY (repo_id) REFERENCES repo_github (id),
    CONSTRAINT snapshot_pk PRIMARY KEY (sha, repo_id)
);

CREATE TABLE IF NOT EXISTS pull
(
    id                    BIGINT PRIMARY KEY,
    number                INT                      NOT NULL,
    title                 VARCHAR(128)             NOT NULL,
    open                  BOOLEAN                  NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL,

    head_snapshot_sha     CHAR(40)                 NOT NULL,
    head_snapshot_repo_id BIGINT                   NOT NULL,

    base_snapshot_sha     CHAR(40)                 NOT NULL,
    base_snapshot_repo_id BIGINT                   NOT NULL,

    project_id            BIGINT                   NOT NULL,
    author_github_id      BIGINT                   NOT NULL,

    FOREIGN KEY (head_snapshot_sha, head_snapshot_repo_id) REFERENCES snapshot (sha, repo_id),
    FOREIGN KEY (base_snapshot_sha, base_snapshot_repo_id) REFERENCES snapshot (sha, repo_id),
    FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE,
    FOREIGN KEY (author_github_id) REFERENCES user_github (id)
);

CREATE TABLE IF NOT EXISTS snapshot_pull
(
    snapshot_sha     CHAR(40) NOT NULL,
    snapshot_repo_id BIGINT   NOT NULL,
    pull_id          BIGINT   NOT NULL,

    FOREIGN KEY (snapshot_sha, snapshot_repo_id) REFERENCES snapshot (sha, repo_id),
    FOREIGN KEY (pull_id) REFERENCES pull (id) ON DELETE CASCADE,
    CONSTRAINT snapshot_pull_pk PRIMARY KEY (snapshot_sha, snapshot_repo_id, pull_id)
);

--  TODO: extract (target | source)_ ... into separate table
CREATE TABLE IF NOT EXISTS clone
(
    id                BIGSERIAL PRIMARY KEY,
    target_commit_sha CHAR(40)     NOT NULL,
    target_repo_id    BIGINT       NOT NULL,
    target_file       VARCHAR(256) NOT NULL,
    target_from_line  INT          NOT NULL,
    target_to_line    INT          NOT NULL,
    source_commit_sha CHAR(40)     NOT NULL,
    source_repo_id    BIGINT       NOT NULL,
    source_file       VARCHAR(256) NOT NULL,
    source_from_line  INT          NOT NULL,
    source_to_line    INT          NOT NULL,
    suppressed        BOOLEAN      NOT NULL DEFAULT FALSE,

    FOREIGN KEY (target_commit_sha, target_repo_id) REFERENCES snapshot (sha, repo_id),
    FOREIGN KEY (source_commit_sha, source_repo_id) REFERENCES snapshot (sha, repo_id)
);
