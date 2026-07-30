CREATE TABLE `orders`
(
    id               varchar(36)    NOT NULL,
    version          INTEGER                 DEFAULT 0,
    customer_name    VARCHAR(100)   NOT NULL,
    customer_phone   VARCHAR(15)    NOT NULL,
    customer_email   VARCHAR(100),
    shipping_address TEXT           NOT NULL,
    note             TEXT,
    total_amount     decimal(15, 2) NOT NULL DEFAULT 0,
    payment_method   VARCHAR(50)             DEFAULT 'COD',
    status           ENUM('PENDING', 'CONFIRMED', 'SHIPPING', 'COMPLETED', 'CANCELLED') DEFAULT 'PENDING',
    del_flag         integer                 DEFAULT 0,
    created_date     timestamp(6),
    updated_date     timestamp(6),
    PRIMARY KEY (id)
);

CREATE TABLE order_detail
(
    id                 varchar(36)    NOT NULL,
    version            INTEGER                 DEFAULT 0,
    order_id           varchar(36)    NOT NULL,
    product_id varchar(36)    NOT NULL,
    price              decimal(15, 2) NOT NULL DEFAULT 0,
    quantity           INTEGER        NOT NULL,
    del_flag           integer                 DEFAULT 0,
    created_date       timestamp(6),
    updated_date       timestamp(6),
    PRIMARY KEY (id)
);