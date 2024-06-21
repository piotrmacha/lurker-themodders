package pl.piotrmacha.lurker.domain;

import lombok.With;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;
import org.springframework.stereotype.Component;
import pl.piotrmacha.lurker.jooq.Tables;
import pl.piotrmacha.lurker.jooq.tables.records.TopicRecord;

import java.time.Instant;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

@With
public record Topic(
        Long id,
        String oid,
        String url,
        String title,
        Long authorId,
        Long boardId,
        Instant createdAt,
        Instant lastUpdate
) {
    private static TopicDao dao;

    public Topic save() {
        return dao.save(this.withLastUpdate(Instant.now()));
    }

    public static TopicDao dao() {
        return dao;
    }

    public static Topic of(Long id, String oid, String url, String title, Long authorId, Long boardId, Instant createdAt, Instant lastUpdate) {
        return new Topic(id, oid, url, title, authorId, boardId, createdAt, lastUpdate);
    }

    public static Topic of(String oid, String url, String title, Long authorId, Long boardId, Instant createdAt) {
        return of(null, oid, url, title, authorId, boardId, createdAt, Instant.now());
    }

    public static Topic of(String oid, String url, Long boardId) {
        return of(oid, url, null, null, boardId, null);
    }

    @Component
    public static class TopicDao extends AbstractDao<Topic, TopicRecord, Long> {
        public TopicDao(DSLContext context) {
            super(context);
            dao = this;
        }

        public Optional<Topic> findByOid(String oid) {
            return findBy(s -> s.where(Tables.TOPIC.OID.eq(oid)));
        }

        public Topic getByOid(String oid) {
            return findByOid(oid).orElseThrow(() -> new RuntimeException("Entity not found"));
        }

        @Override
        public Topic save(Topic entity) {
            return findByOid(entity.oid)
                    .map(existing -> entity.withId(existing.id()))
                    .orElseGet(() -> {
                       if (entity.id() == null) {
                            return super.create(entity.withId(context.nextval("topic_id_seq").longValue()));
                       }
                       return super.create(entity);
                    });
        }

        @Override
        public Topic create(Topic entity) {
            if (entity.id() == null) {
                return super.create(entity.withId(context.nextval("topic_id_seq").longValue()));
            }
            return super.create(entity);
        }

        @Override
        protected Class<Topic> entity() {
            return Topic.class;
        }

        @Override
        protected Table<TopicRecord> table() {
            return Tables.TOPIC;
        }

        @Override
        protected TableField<TopicRecord, Long> idField() {
            return Tables.TOPIC.ID;
        }

        @Override
        protected BiFunction<RecordMapper<TopicRecord>, Topic, RecordMapper<TopicRecord>> mapper() {
            return (mapper, topic) -> mapper
                    .set(Tables.TOPIC.ID, topic.id())
                    .set(Tables.TOPIC.OID, topic.oid())
                    .set(Tables.TOPIC.URL, topic.url())
                    .set(Tables.TOPIC.TITLE, topic.title())
                    .set(Tables.TOPIC.AUTHOR_ID, topic.authorId())
                    .set(Tables.TOPIC.BOARD_ID, topic.boardId())
                    .set(Tables.TOPIC.CREATED_AT, of(topic.createdAt()))
                    .set(Tables.TOPIC.LAST_UPDATE, of(topic.lastUpdate()));
        }

        @Override
        protected Function<Topic, Long> idMapper() {
            return Topic::id;
        }
    }
}
