DROP TABLE IF EXISTS policy CASCADE;

CREATE TABLE policy
(
    id                  VARCHAR(36)  NOT NULL,
    version             INTEGER DEFAULT 0,
    package_name        VARCHAR(255) NOT NULL,
    policies            JSON,
    after_sales         JSON,
    is_active    INTEGER      DEFAULT 0,
    del_flag     INTEGER      DEFAULT 0,
    created_date        TIMESTAMP(6),
    updated_date        TIMESTAMP(6),
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;