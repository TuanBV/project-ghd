drop table if exists contact cascade;
CREATE TABLE contact
(
    id           VARCHAR(36)  NOT NULL,
    version      INTEGER,
    full_name    VARCHAR(255) NOT NULL,
    phone        VARCHAR(20)  NOT NULL,
    service_type VARCHAR(100),
    message      TEXT,
    status       VARCHAR(50) DEFAULT 'NEW',
    note         TEXT,
    del_flag     integer     DEFAULT 0,
    created_date timestamp(6),
    updated_date timestamp(6),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_contact_phone ON contact (phone);
CREATE INDEX idx_contact_status ON contact (status);