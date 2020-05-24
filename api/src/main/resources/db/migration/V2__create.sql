CREATE TABLE IF NOT EXISTS pull
(
    id         BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    number     INT    NOT NULL,

    FOREIGN KEY (project_id) REFERENCES project (id)
);
