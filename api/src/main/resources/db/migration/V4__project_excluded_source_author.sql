CREATE TABLE IF NOT EXISTS project_excluded_source_author
(
    project_id                BIGINT NOT NULL,
    excluded_source_author_id BIGINT NOT NULL,

    FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE,
    FOREIGN KEY (excluded_source_author_id) REFERENCES user_github (id),
    CONSTRAINT project_conf_excluded_source_pk PRIMARY KEY (project_id, excluded_source_author_id)
);
