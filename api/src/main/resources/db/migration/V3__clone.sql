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
