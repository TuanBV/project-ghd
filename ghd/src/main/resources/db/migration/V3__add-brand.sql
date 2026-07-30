CREATE TABLE brand
(
    id           varchar(36) not null,
    version      integer DEFAULT 0,
    title        varchar(50) not null unique,
    logo         varchar(500),
    del_flag     integer DEFAULT 0,
    created_date timestamp(6),
    updated_date timestamp(6),
    primary key (id)
);

