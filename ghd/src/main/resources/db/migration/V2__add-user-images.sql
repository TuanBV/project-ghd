CREATE TABLE user_image
(
    id           varchar(36)  not null,
    url          varchar(1000),
    image_order  integer DEFAULT 0,
    del_flag integer DEFAULT 0,
    created_date timestamp(6),
    updated_date timestamp(6),
    user_id      varchar(36),
    primary key (id)
);

