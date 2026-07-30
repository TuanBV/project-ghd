CREATE TABLE product_image
(
    id           varchar(36)  NOT NULL,
    version      integer DEFAULT 0,
    product_id   varchar(36)  NOT NULL,
    image_url    varchar(500) NOT NULL,
    del_flag     integer DEFAULT 0,
    created_date timestamp(6),
    updated_date timestamp(6),
    PRIMARY KEY (id)
);
