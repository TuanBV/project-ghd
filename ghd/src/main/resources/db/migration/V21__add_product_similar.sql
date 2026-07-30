DROP TABLE IF EXISTS product_similar CASCADE;

CREATE TABLE product_similar
(
    id            VARCHAR(36) NOT NULL,
    version       INTEGER DEFAULT 0,
    product_group TEXT,
    del_flag      integer DEFAULT 0,
    created_date  TIMESTAMP(6),
    updated_date  TIMESTAMP(6),
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;