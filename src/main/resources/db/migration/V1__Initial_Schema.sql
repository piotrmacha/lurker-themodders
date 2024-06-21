create table asset
(
    id          bigint primary key,
    name        text                     not null,
    url         text                     not null,
    path        text                     not null,
    mime_type   text                              default 'application/octet-stream',
    size        bigint                            default 0,
    last_update timestamp with time zone not null default now()
);
create sequence asset_id_seq start 1 increment 1;
create unique index asset_url_idx on asset (url);
alter table asset
    alter column id set default nextval('asset_id_seq');

create table account
(
    id          bigint primary key,
    oid         text                     not null,
    url         text                     not null,
    username    text,
    avatar_id   bigint references asset (id),
    last_update timestamp with time zone not null default now()
);
create sequence account_id_seq start 1 increment 1;
create unique index account_oid_idx on account (oid);
alter table account
    alter column id set default nextval('account_id_seq');

create table board
(
    id          bigint primary key,
    oid         text                     not null,
    url         text                     not null,
    name        text,
    description text,
    parent_id   bigint references board (id),
    last_update timestamp with time zone not null default now()
);
create sequence board_id_seq start 1 increment 1;
create unique index board_oid_idx on board (oid);
alter table board
    alter column id set default nextval('board_id_seq');

create table topic
(
    id          bigint primary key,
    oid         text                     not null,
    url         text                     not null,
    title       text,
    author_id   bigint references account (id),
    board_id    bigint references board (id),
    created_at  timestamp with time zone,
    last_update timestamp with time zone not null default now()
);
create sequence topic_id_seq start 1 increment 1;
create unique index topic_oid_idx on topic (oid);
alter table topic
    alter column id set default nextval('topic_id_seq');

create table post
(
    id          bigint primary key,
    oid         text,
    url         text                     not null,
    author_id   bigint references account (id),
    topic_id    bigint references topic (id),
    content     text                     not null,
    created_at  timestamp with time zone,
    last_update timestamp with time zone not null default now()
);
create sequence post_id_seq start 1 increment 1;
create unique index post_oid_idx on post (oid);
alter table post
    alter column id set default nextval('post_id_seq');

create table post_attachment
(
    post_id  bigint references post (id)  not null,
    asset_id bigint references asset (id) not null,
    primary key (post_id, asset_id)
);

create table post_fulltext
(
    post_id               bigint primary key references post (id) not null,
    topic_id              bigint references topic (id)            not null,
    author_id             bigint references account (id)          not null,
    content               text,
    topic                 text,
    author                text,
    search_vector_english tsvector generated always as ( to_tsvector('english', author || ' ' || topic || ' ' || content)) stored,
    search_vector_polish  tsvector generated always as ( to_tsvector('pl_ispell', author || ' ' || topic || ' ' || content)) stored
);
create index post_fulltext_search_vector_english_idx on post_fulltext using gin (search_vector_english);
create index post_fulltext_search_vector_polish_idx on post_fulltext using gin (search_vector_polish);

create table download_queue
(
    id         bigint primary key,
    type       text                     not null check ( type in ('ASSET', 'BOARD', 'TOPIC') ),
    url        text                     not null,
    entity_id  bigint,
    locked_at  timestamp with time zone,
    created_at timestamp with time zone not null default now()
);
create sequence download_queue_id_seq start 1 increment 1;
alter table download_queue
    alter column id set default nextval('download_queue_id_seq');

create table download_queue_done
(
    id         bigint primary key,
    type       text                     not null check ( type in ('ASSET', 'BOARD', 'TOPIC') ),
    url        text                     not null,
    entity_id  bigint,
    created_at timestamp with time zone not null default now(),
    done_at    timestamp with time zone not null default now()
);

create table download_queue_failure
(
    id         bigint primary key,
    type       text                     not null check ( type in ('ASSET', 'BOARD', 'TOPIC') ),
    url        text                     not null,
    entity_id  bigint,
    error      text,
    message    text,
    created_at timestamp with time zone not null default now(),
    failed_at  timestamp with time zone not null default now()
);

create table download_queue_scheduled
(
    id           bigint primary key,
    type         text                     not null check ( type in ('ASSET', 'BOARD', 'TOPIC') ),
    url          text                     not null,
    entity_id    bigint,
    created_at   timestamp with time zone not null default now(),
    scheduled_at timestamp with time zone not null
);
create sequence download_queue_scheduled_id_seq start 1 increment 1;
alter table download_queue_scheduled
    alter column id set default nextval('download_queue_scheduled_id_seq');