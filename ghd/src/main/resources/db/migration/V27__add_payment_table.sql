CREATE TABLE payment
(
    id                     VARCHAR(36)    NOT NULL,
    version                INT                     DEFAULT 0,
    order_id               VARCHAR(36)    NOT NULL,
    amount                 DECIMAL(15, 2) NOT NULL,
    provider               VARCHAR(20)    NOT NULL DEFAULT 'VNPAY',
    payment_method         VARCHAR(20)    NOT NULL,
    txn_ref                VARCHAR(34)    NOT NULL,
    gateway_transaction_id VARCHAR(50)    NULL,
    bank_code              VARCHAR(20)    NULL,
    response_code          VARCHAR(10)    NULL,
    status                 VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    ipn_received_at        TIMESTAMP(6)   NULL,
    raw_ipn_payload        TEXT           NULL,
    del_flag               INT                     DEFAULT 0,
    created_date           TIMESTAMP(6)   NULL,
    updated_date           TIMESTAMP(6)   NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE UNIQUE INDEX uq_payment_txn_ref ON payment (txn_ref);
CREATE INDEX idx_payment_order_id ON payment (order_id);
