package pl.piotrmacha.lurker.domain;

import lombok.With;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;
import org.springframework.stereotype.Component;
import pl.piotrmacha.lurker.jooq.Tables;
import pl.piotrmacha.lurker.jooq.tables.records.BoardRecord;

import java.time.Instant;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

@With
public record Board(
        Long id,
        String oid,
        String url,
        String name,
        String description,
        Long parentId,
        Instant lastUpdate
) {
    private static BoardDao dao;

    public Board save() {
        return dao().save(this.withLastUpdate(Instant.now()));
    }

    public static BoardDao dao() {
        return dao;
    }

    public static Board of(Long id, String oid, String url, String name, String description, Long parentId, Instant lastUpdate) {
        return new Board(id, oid, url, name, description, parentId, lastUpdate);
    }

    public static Board of(String oid, String url, String name, String description, Long parentId) {
        return of(null, oid, url, name, description, parentId, Instant.now());
    }

    public static Board of(String oid, String url, String name, String description) {
        return of(oid, url, name, description, null);
    }

    @Component
    public static class BoardDao extends AbstractDao<Board, BoardRecord, Long> {
        public BoardDao(DSLContext context) {
            super(context);
            dao = this;
        }

        public Optional<Board> findByOid(String oid) {
            return findBy(s -> s.where(Tables.BOARD.OID.eq(oid)));
        }

        public Board getByOid(String oid) {
            return findByOid(oid).orElseThrow(() -> new RuntimeException("Entity not found"));
        }

        @Override
        public Board save(Board entity) {
            return findByOid(entity.oid())
                    .map(existing -> update(entity.withId(existing.id())))
                    .orElseGet(() -> {
                        if (entity.id() == null) {
                            return super.create(entity.withId(context.nextval("board_id_seq").longValue()));
                        }
                        return super.create(entity);
                    });
        }

        @Override
        protected Class<Board> entity() {
            return Board.class;
        }

        @Override
        protected Table<BoardRecord> table() {
            return Tables.BOARD;
        }

        @Override
        protected TableField<BoardRecord, Long> idField() {
            return Tables.BOARD.ID;
        }

        @Override
        protected BiFunction<RecordMapper<BoardRecord>, Board, RecordMapper<BoardRecord>> mapper() {
            return (mapper, board) -> mapper
                    .set(Tables.BOARD.ID, board.id())
                    .set(Tables.BOARD.OID, board.oid())
                    .set(Tables.BOARD.URL, board.url())
                    .set(Tables.BOARD.NAME, board.name())
                    .set(Tables.BOARD.DESCRIPTION, board.description())
                    .set(Tables.BOARD.PARENT_ID, board.parentId())
                    .set(Tables.BOARD.LAST_UPDATE, of(board.lastUpdate()));
        }

        @Override
        protected Function<Board, Long> idMapper() {
            return Board::id;
        }
    }
}
