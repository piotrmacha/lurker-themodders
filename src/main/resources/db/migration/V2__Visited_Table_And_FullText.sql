create table visited_url
(
    url        text primary key,
    visited_at timestamp with time zone not null
);

create index post_fulltext_polish_idx ON post USING gin (to_tsvector('pl_ispell', content));
create index thread_fulltext_polish_idx ON thread USING gin (to_tsvector('pl_ispell', title));