CREATE TYPE code_language_enum AS ENUM ('JAVA', 'KOTLIN');

ALTER TABLE project_conf
    ADD COLUMN languages code_language_enum[] NOT NULL DEFAULT ARRAY ['JAVA']::code_language_enum[];
