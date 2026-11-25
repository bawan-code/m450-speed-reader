-- V1: the four tables exactly as the JPA entities define them (model/*.java).
-- Taken from Hibernate's own DDL for these entities; Hibernate runs with ddl-auto=validate.
-- No extra constraints (e.g. CHECK): the integration tests must show the real state of the code.
-- Never edit this file after it is merged - add V2, V3, ... instead.

CREATE TABLE users (
    id       BIGINT       NOT NULL AUTO_INCREMENT,
    username VARCHAR(255),
    email    VARCHAR(255),
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE texts (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    title      VARCHAR(255),
    content    LONGTEXT,
    word_count INT          NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE reading_sessions (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    user_id             BIGINT      NOT NULL,
    text_id             BIGINT      NOT NULL,
    wpm                 INT         NOT NULL,
    words_read          INT         NOT NULL,
    duration_seconds    INT         NOT NULL,
    date                DATETIME(6),
    comprehension_score INT,
    PRIMARY KEY (id),
    CONSTRAINT fk_reading_sessions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_reading_sessions_text FOREIGN KEY (text_id) REFERENCES texts (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE bookmarks (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    user_id       BIGINT NOT NULL,
    text_id       BIGINT NOT NULL,
    word_position INT    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_bookmarks_user_text UNIQUE (user_id, text_id),
    CONSTRAINT fk_bookmarks_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_bookmarks_text FOREIGN KEY (text_id) REFERENCES texts (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
