create table asset
(
    id   bigint primary key,
    name text  not null,
    url  text  not null,
    data bytea
);
create sequence asset_id_seq start 1 increment 1;

create table account
(
    id       text primary key,
    username text not null,
    url      text not null,
    avatar   bigint references asset (id)
);

create table category
(
    id          text primary key,
    name        text not null,
    url         text not null,
    description text,
    parent      text references category (id)
);

create table thread
(
    id         text primary key,
    title      text                     not null,
    author     text references account (id),
    category   text references category (id),
    url        text                     not null,
    created_at timestamp with time zone not null
);

create table post
(
    id         text primary key,
    author     text references account (id),
    thread     text references thread (id),
    content    text                     not null,
    url        text                     not null,
    created_at timestamp with time zone not null
);