create table if not exists positions(
    num serial
);

create table if not exists tables
(
    id           serial primary key,
    name         varchar(255) unique,
    participants int not null default 0,
    position     bigint not null default nextval('positions_num_seq') --used this field for fast fetch data in order
);

create table if not exists users(
    username varchar(255) unique not null,
    passHash varchar not null,
    user_type varchar not null
);