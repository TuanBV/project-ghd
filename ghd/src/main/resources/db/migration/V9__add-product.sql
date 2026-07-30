CREATE TABLE product
(
    id            varchar(36)  NOT NULL,
    version       integer DEFAULT 0,
    category_id   varchar(36)  NOT NULL, -- Chỉ lưu ID, không tạo FK
    brand_id      varchar(36)  NOT NULL, -- Chỉ lưu ID, không tạo FK
    title         varchar(255) NOT NULL,
    content       longtext,
    specification longtext,
    status        integer DEFAULT 1,
    del_flag      integer DEFAULT 0,
    created_date  timestamp(6),
    updated_date  timestamp(6),
    PRIMARY KEY (id)
);
