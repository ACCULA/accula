CREATE TABLE IF NOT EXISTS commit
(
    id    BIGSERIAL PRIMARY KEY,
    owner VARCHAR(39)     NOT NULL,
    repo  VARCHAR(256)    NOT NULL,
    sha   CHAR(40) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS pull
(
    id                   BIGSERIAL PRIMARY KEY,
    project_id           BIGINT                   NOT NULL,
    number               INT                      NOT NULL,
    head_last_commit_id  BIGINT                   NOT NULL,
    base_last_commit_sha CHAR(40)                 NOT NULL,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL,

    FOREIGN KEY (project_id)          REFERENCES project (id),
    FOREIGN KEY (head_last_commit_id) REFERENCES commit (id)
);
