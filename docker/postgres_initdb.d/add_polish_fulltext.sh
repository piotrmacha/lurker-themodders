#!/usr/bin/env bash
set -e

#SOURCE="/var/postgres_polish_fulltext"
#TARGET="`pg_config --sharedir`/tsearch_data"
#sudo cp -r $SOURCE/* $TARGET/

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
CREATE TEXT SEARCH DICTIONARY pl_ispell (
  Template = ispell,
  DictFile = polish,
  AffFile = polish,
  StopWords = polish
);

CREATE TEXT SEARCH CONFIGURATION pl_ispell(parser = default);

ALTER TEXT SEARCH CONFIGURATION pl_ispell
  ALTER MAPPING FOR asciiword, asciihword, hword_asciipart, word, hword, hword_part
  WITH pl_ispell;
EOSQL