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

CREATE TYPE project_state_enum AS ENUM ('CONFIGURING', 'CONFIGURED');

CREATE TABLE IF NOT EXISTS project
(
    id             BIGSERIAL PRIMARY KEY,
    state          project_state_enum NOT NULL DEFAULT 'CONFIGURING',
    github_repo_id BIGINT UNIQUE      NOT NULL,
    creator_id     BIGINT             NOT NULL,

    FOREIGN KEY (github_repo_id) REFERENCES repo_github (id),
    FOREIGN KEY (creator_id) REFERENCES user_ (id)
);

CREATE TABLE IF NOT EXISTS project_repo
(
    project_id BIGINT NOT NULL,
    repo_id    BIGINT NOT NULL,

    FOREIGN KEY (project_id) REFERENCES project (id),
    FOREIGN KEY (repo_id) REFERENCES repo_github (id),
    CONSTRAINT project_repo_pk PRIMARY KEY (project_id, repo_id)
);

CREATE TABLE IF NOT EXISTS project_admin
(
    project_id BIGINT NOT NULL,
    admin_id   BIGINT NOT NULL,

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
    CONSTRAINT snapshot_pk PRIMARY KEY (sha, repo_id, branch)
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
    head_snapshot_branch  VARCHAR(256)             NOT NULL,

    base_snapshot_sha     CHAR(40)                 NOT NULL,
    base_snapshot_repo_id BIGINT                   NOT NULL,
    base_snapshot_branch  VARCHAR(256)             NOT NULL,

    author_github_id      BIGINT                   NOT NULL,

    FOREIGN KEY (head_snapshot_sha, head_snapshot_repo_id, head_snapshot_branch) REFERENCES snapshot (sha, repo_id, branch),
    FOREIGN KEY (base_snapshot_sha, base_snapshot_repo_id, base_snapshot_branch) REFERENCES snapshot (sha, repo_id, branch),
    FOREIGN KEY (author_github_id) REFERENCES user_github (id)
);

CREATE TABLE IF NOT EXISTS snapshot_pull
(
    snapshot_sha     CHAR(40)     NOT NULL,
    snapshot_repo_id BIGINT       NOT NULL,
    snapshot_branch  VARCHAR(256) NOT NULL,
    pull_id          BIGINT       NOT NULL,

    FOREIGN KEY (snapshot_sha, snapshot_repo_id, snapshot_branch) REFERENCES snapshot (sha, repo_id, branch),
    FOREIGN KEY (pull_id) REFERENCES pull (id) ON DELETE CASCADE,
    CONSTRAINT snapshot_pull_pk PRIMARY KEY (snapshot_sha, snapshot_repo_id, snapshot_branch, pull_id)
);

CREATE TABLE IF NOT EXISTS clone_snippet
(
    id         BIGSERIAL PRIMARY KEY,
    commit_sha CHAR(40)     NOT NULL,
    repo_id    BIGINT       NOT NULL,
    branch     VARCHAR(256) NOT NULL,
    pull_id    BIGINT       NOT NULL,

    file       TEXT         NOT NULL,
    from_line  INT          NOT NULL,
    to_line    INT          NOT NULL,

    FOREIGN KEY (commit_sha, repo_id, branch, pull_id) REFERENCES
        snapshot_pull (snapshot_sha, snapshot_repo_id, snapshot_branch, pull_id)
);

CREATE TABLE IF NOT EXISTS clone
(
    id         BIGSERIAL PRIMARY KEY,
    target_id  BIGINT  NOT NULL,
    source_id  BIGINT  NOT NULL,

    suppressed BOOLEAN NOT NULL DEFAULT FALSE,

    FOREIGN KEY (target_id) REFERENCES clone_snippet (id) ON DELETE CASCADE,
    FOREIGN KEY (source_id) REFERENCES clone_snippet (id) ON DELETE CASCADE
);
