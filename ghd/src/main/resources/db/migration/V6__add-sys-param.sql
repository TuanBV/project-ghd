DROP TABLE IF EXISTS sys_param CASCADE;

CREATE TABLE sys_param
(
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    version      INTEGER,
    param_key    VARCHAR(100) NOT NULL UNIQUE,
    param_value  TEXT         NOT NULL,
    param_name   VARCHAR(255) NOT NULL,
    group_code   VARCHAR(50),
    description  VARCHAR(500),
    del_flag     INTEGER      DEFAULT 0,
    created_date TIMESTAMP(6),
    updated_date TIMESTAMP(6),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_param_key ON sys_param (param_key);