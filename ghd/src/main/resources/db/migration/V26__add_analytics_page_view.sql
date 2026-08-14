CREATE TABLE page_view
(
    id            VARCHAR(36)  NOT NULL,
    visitor_id    VARCHAR(64)  NOT NULL,
    session_id    VARCHAR(64)  NOT NULL,
    url           VARCHAR(500) NOT NULL,
    referrer_host VARCHAR(255) NULL,
    ip_hash       VARCHAR(64)  NULL,
    user_agent    VARCHAR(500) NULL,
    device_type   VARCHAR(20)  NOT NULL DEFAULT 'DESKTOP',
    browser       VARCHAR(50)  NULL,
    created_date  DATETIME     NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_page_view_created_date ON page_view (created_date);
CREATE INDEX idx_page_view_url ON page_view (url(191), created_date);
CREATE INDEX idx_page_view_referrer ON page_view (referrer_host, created_date);
CREATE INDEX idx_page_view_visitor ON page_view (visitor_id, created_date);
CREATE INDEX idx_page_view_session ON page_view (session_id);
