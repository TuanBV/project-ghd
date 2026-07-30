CREATE TABLE category
(
    id           varchar(36) not null,
    version      integer DEFAULT 0,
    title        varchar(50) not null unique,
    logo         varchar(500),
    priority     integer DEFAULT 0 CHECK (priority >= 0),
    del_flag     integer DEFAULT 0,
    created_date timestamp(6),
    updated_date timestamp(6),
    primary key (id)
);

