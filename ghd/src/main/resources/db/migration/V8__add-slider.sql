DROP TABLE IF EXISTS slider CASCADE;

CREATE TABLE slider
(
    id           VARCHAR(36)  NOT NULL,
    version      INTEGER DEFAULT 0,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    image_url    VARCHAR(500) NOT NULL,
    link_url     VARCHAR(255),
    position     INT     DEFAULT 1,
    is_active    BOOLEAN DEFAULT TRUE,
    del_flag     integer DEFAULT 0,
    created_date timestamp(6),
    updated_date timestamp(6),

    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
