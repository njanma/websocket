create table if not exists ordering(
    num serial
);

create table if not exists "table"
(
    id           serial primary key,
    name         varchar(255) unique,
    participants int not null default 0,
    ordering     bigint not null default nextval('ordering_num_seq')
);

create table if not exists users(
    username varchar(255) unique not null,
    passHash varchar not null,
    user_type varchar not null
);