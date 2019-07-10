create table "table"
(
    id           serial primary key,
    name         varchar(255) unique,
    participants int not null default 0,
    ordering     int not null
);