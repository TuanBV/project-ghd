DROP TABLE IF EXISTS banner CASCADE;

CREATE TABLE banner
(
    id           VARCHAR(36)  NOT NULL,
    version      INTEGER      DEFAULT 0,
    title        VARCHAR(255) NOT NULL,
    image_url    VARCHAR(500) NOT NULL,
    link_url     VARCHAR(255),
    position     VARCHAR(50) COMMENT 'Vị trí hiển thị (VD: HOME_HEADER, SIDEBAR)',
    start_date   TIMESTAMP(6) NULL COMMENT 'Thời gian bắt đầu hiển thị',
    end_date     TIMESTAMP(6) NULL COMMENT 'Thời gian kết thúc hiển thị',
    is_active    INTEGER      DEFAULT 0,
    del_flag     INTEGER      DEFAULT 0,
    created_date TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_date TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;