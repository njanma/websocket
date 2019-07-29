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