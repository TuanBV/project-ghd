DROP TABLE IF EXISTS news CASCADE;

CREATE TABLE news
(
    id            VARCHAR(36)  NOT NULL,
    version       INTEGER      DEFAULT 0,

    title         VARCHAR(255) NOT NULL,
    slug          VARCHAR(255) NOT NULL,
    summary       VARCHAR(1000),
    content       LONGTEXT     NOT NULL,
    thumbnail     VARCHAR(500),

    category_id   VARCHAR(36),
    status        VARCHAR(20)  DEFAULT 'DRAFT',
    post_type     VARCHAR(20)  DEFAULT 'NEWS',

    -- Hỗ trợ SEO
    meta_title    VARCHAR(255),
    meta_keyword  VARCHAR(255),
    meta_desc     VARCHAR(1000),

    view_count    INTEGER      DEFAULT 0,
    is_featured   BIT(1)       DEFAULT 0,
    author_id     VARCHAR(36),

    del_flag     integer DEFAULT 0,
    created_date timestamp(6),
    updated_date timestamp(6),

    PRIMARY KEY (id),
    CONSTRAINT uk_news_slug UNIQUE (slug)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_news_slug ON news (slug);
CREATE INDEX idx_news_status ON news (status);
CREATE INDEX idx_news_category ON news (category_id);