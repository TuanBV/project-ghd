drop table if exists user cascade;
create table user
(
    version      integer,
    created_date timestamp(6),
    updated_date timestamp(6),
    phone        varchar(15),
    id           varchar(36)  not null,
    username     varchar(50)  not null unique,
    email        varchar(100) not null unique,
    password     varchar(255),
    del_flag     integer DEFAULT 0,
    role INTEGER DEFAULT 1,
    primary key (id)
);