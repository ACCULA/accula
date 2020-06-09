CREATE TABLE IF NOT EXISTS user_github
(
    id     BIGINT PRIMARY KEY,
    login  VARCHAR(39) UNIQUE    NOT NULL,
    name   VARCHAR(256),
    avatar VARCHAR(2000)         NOT NULL,
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

CREATE TABLE IF NOT EXISTS project
(
    id             BIGSERIAL PRIMARY KEY,
    github_repo_id BIGINT UNIQUE NOT NULL,
    creator_id     BIGINT        NOT NULL,

    FOREIGN KEY (github_repo_id) REFERENCES repo_github (id),
    FOREIGN KEY (creator_id) REFERENCES user_ (id)
);

CREATE TABLE IF NOT EXISTS project_admin
(
    project_id BIGSERIAL NOT NULL,
    admin_id   BIGSERIAL NOT NULL,

    FOREIGN KEY (project_id) REFERENCES project (id),
    FOREIGN KEY (admin_id) REFERENCES user_ (id),
    CONSTRAINT project_admin_pk PRIMARY KEY (project_id, admin_id)
);

CREATE TABLE IF NOT EXISTS commit
(
    sha CHAR(40) PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS commit_snapshot
(
    sha     CHAR(40)     NOT NULL,
    repo_id BIGINT       NOT NULL,
    branch  VARCHAR(256) NOT NULL,

    FOREIGN KEY (sha) REFERENCES commit (sha),
    FOREIGN KEY (repo_id) REFERENCES repo_github (id),
    CONSTRAINT commit_snapshot_pk PRIMARY KEY (sha, repo_id)
);

CREATE TABLE IF NOT EXISTS pull
(
    id                           BIGINT PRIMARY KEY,
    number                       INT                      NOT NULL,
    title                        VARCHAR(128)             NOT NULL,
    open                         BOOLEAN                  NOT NULL,
    created_at                   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                   TIMESTAMP WITH TIME ZONE NOT NULL,

    head_commit_snapshot_sha     CHAR(40)                 NOT NULL,
    head_commit_snapshot_repo_id BIGINT                   NOT NULL,

    base_commit_snapshot_sha     CHAR(40)                 NOT NULL,
    base_commit_snapshot_repo_id BIGINT                   NOT NULL,

    project_id                   BIGINT                   NOT NULL,
    author_github_id             BIGINT                   NOT NULL,

    FOREIGN KEY (head_commit_snapshot_sha, head_commit_snapshot_repo_id) REFERENCES commit_snapshot (sha, repo_id),
    FOREIGN KEY (base_commit_snapshot_sha, base_commit_snapshot_repo_id) REFERENCES commit_snapshot (sha, repo_id),
    FOREIGN KEY (project_id) REFERENCES project (id),
    FOREIGN KEY (author_github_id) REFERENCES user_github (id)
);

CREATE TABLE IF NOT EXISTS commit_snapshot_pull
(
    commit_snapshot_sha     CHAR(40) NOT NULL,
    commit_snapshot_repo_id BIGINT   NOT NULL,
    pull_id                 BIGINT   NOT NULL,

    FOREIGN KEY (commit_snapshot_sha, commit_snapshot_repo_id) REFERENCES commit_snapshot (sha, repo_id),
    FOREIGN KEY (pull_id) REFERENCES pull (id),
    CONSTRAINT commit_snapshot_pull_pk PRIMARY KEY (commit_snapshot_sha, commit_snapshot_repo_id, pull_id)
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

    FOREIGN KEY (target_commit_sha, target_repo_id) REFERENCES commit_snapshot (sha, repo_id),
    FOREIGN KEY (source_commit_sha, source_repo_id) REFERENCES commit_snapshot (sha, repo_id)
);
