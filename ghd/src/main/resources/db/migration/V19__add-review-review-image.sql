CREATE TABLE review
(
    id           varchar(36)  not null,
    version      integer      DEFAULT 0,
    review_name  varchar(255) not null,
    review_email varchar(255) not null,
    content      varchar(1000) not null,
    product_id varchar(36)  not null,
    rating       integer,
    del_flag     integer      DEFAULT 0 not null,
    created_date timestamp(6),
    updated_date timestamp(6),
    primary key (id)
);
CREATE TABLE review_image
(
    id                 varchar(36)  not null,
    version            integer      DEFAULT 0,
    review_id varchar(36)  not null,
    image_url          varchar(255) not null,
    del_flag           integer      DEFAULT 0 not null,
    created_date       timestamp(6),
    updated_date       timestamp(6),
    primary key (id)
);