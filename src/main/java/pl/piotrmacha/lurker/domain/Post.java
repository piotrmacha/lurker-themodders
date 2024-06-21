package pl.piotrmacha.lurker.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;
import lombok.experimental.Accessors;
import org.jooq.*;
import org.jooq.exception.IntegrityConstraintViolationException;
import org.jooq.exception.NoDataFoundException;
import org.springframework.stereotype.Component;
import pl.piotrmacha.lurker.jooq.Tables;
import pl.piotrmacha.lurker.jooq.tables.records.PostRecord;

import java.time.Instant;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.jooq.impl.DSL.field;

@With
public record Post(
        Long id,
        String oid,
        String url,
        Long authorId,
        Long topicId,
        String content,
        Instant createdAt,
        Instant lastUpdate
) {
    private static PostDao dao;

    public Post save() {
        return dao().save(this.withLastUpdate(Instant.now()));
    }

    public static PostDao dao() {
        return dao;
    }

    public static Post of(Long id, String oid, String url, Long authorId, Long topicId, String content, Instant createdAt, Instant lastUpdate) {
        return new Post(id, oid, url, authorId, topicId, content, createdAt, lastUpdate);
    }

    public static Post of(String oid, String url, Long authorId, Long topicId, String content, Instant createdAt) {
        return of(null, oid, url, authorId, topicId, content, createdAt, Instant.now());
    }

    record Attachment(Long postId, Long assetId) {
        public static Attachment of(Long postId, Long assetId) {
            return new Attachment(postId, assetId);
        }
    }

    record FullText(
            Long postId,
            Long topicId,
            Long authorId,
            String content,
            String topic,
            String author
    ) {
        public static FullText of(Post post) {
            return new FullText(
                    post.id(),
                    post.topicId(),
                    post.authorId(),
                    post.content(),
                    null,
                    null
            );
        }
    }

    @RequiredArgsConstructor
    public enum SearchLanguage {
        ENGLISH("english"),
        POLISH("pl_ispell");

        @Getter
        @Accessors(fluent = true)
        private final String tokenizer;
    }

    @Component
    public static class PostDao extends AbstractDao<Post, PostRecord, Long> {
        private final Topic.TopicDao topicDao;
        private final Account.AccountDao accountDao;

        public PostDao(DSLContext context, Topic.TopicDao topicDao, Account.AccountDao accountDao) {
            super(context);
            this.topicDao = topicDao;
            this.accountDao = accountDao;
            dao = this;
        }

        @Override
        public Post save(Post entity) {
            Post post = super.save(entity);
            return saveFullText(post);
        }

        public Optional<Post> findByOid(String oid) {
            return findBy(s -> s.where(Tables.POST.OID.eq(oid)));
        }

        public Post getByOid(String oid) {
            return findByOid(oid).orElseThrow(() -> new RuntimeException("Entity not found"));
        }

        @Override
        public Post create(Post entity) {
            if (entity.id() == null) {
                return super.create(entity.withId(context.nextval("post_id_seq").longValue()));
            }
            return super.create(entity);
        }

        public Stream<Post> search(String query, SearchLanguage language) {
            String tokenizer = language.tokenizer();
            return context.select(
                            Tables.POST.ID,
                            Tables.POST.OID,
                            Tables.POST.URL,
                            Tables.POST.AUTHOR_ID,
                            Tables.POST.TOPIC_ID,
                            Tables.POST.CONTENT,
                            Tables.POST.CREATED_AT,
                            Tables.POST.LAST_UPDATE,
                            field("ts_rank_cd(to_tsvector('" + tokenizer + "', content), to_tsquery('" + tokenizer + "', 'środek'))", "score"))
                    .from(Tables.POST_FULLTEXT
                            .join(Tables.POST).on(
                                    Tables.POST.ID.eq(Tables.POST_FULLTEXT.POST_ID)))
                    .where("to_tsvector('" + tokenizer + "', content) @@ websearch_to_tsquery('" + tokenizer + "', ?)", query)
                    .stream()
                    .map(r -> r.into(Post.class));
        }

        public Post linkAsset(Post post, Long assetId) {
            try {
                context.selectFrom(Tables.POST_ATTACHMENT)
                        .where(Tables.POST_ATTACHMENT.POST_ID.eq(post.id()))
                        .and(Tables.POST_ATTACHMENT.ASSET_ID.eq(assetId))
                        .fetchSingleInto(Attachment.class);
            } catch (NoDataFoundException e) {
                try {
                    context.insertInto(Tables.POST_ATTACHMENT)
                            .set(Tables.POST_ATTACHMENT.POST_ID, post.id())
                            .set(Tables.POST_ATTACHMENT.ASSET_ID, assetId)
                            .execute();
                } catch (IntegrityConstraintViolationException ee) {
                    return post;
                }
            }
            return post;
        }

        public Post saveFullText(Post post) {
            Topic topic = topicDao.find(post.topicId()).orElse(Topic.of("", "", null));
            Account account = accountDao.find(post.authorId()).orElse(Account.of("", "", null));

            try {
                context.select(field("1"))
                        .from(Tables.POST_FULLTEXT)
                        .where(Tables.POST_FULLTEXT.POST_ID.eq(post.id()))
                        .fetch();
                context.update(Tables.POST_FULLTEXT)
                        .set(Tables.POST_FULLTEXT.CONTENT, post.content())
                        .set(Tables.POST_FULLTEXT.TOPIC, topic.title())
                        .set(Tables.POST_FULLTEXT.AUTHOR, account.username())
                        .where(Tables.POST_FULLTEXT.POST_ID.eq(post.id()))
                        .execute();
            } catch (NoDataFoundException e) {
                context.insertInto(Tables.POST_FULLTEXT)
                        .set(Tables.POST_FULLTEXT.POST_ID, post.id())
                        .set(Tables.POST_FULLTEXT.TOPIC_ID, post.topicId())
                        .set(Tables.POST_FULLTEXT.AUTHOR_ID, post.authorId())
                        .set(Tables.POST_FULLTEXT.CONTENT, post.content())
                        .set(Tables.POST_FULLTEXT.TOPIC, topic.title())
                        .set(Tables.POST_FULLTEXT.AUTHOR, account.username())
                        .execute();
            }

            return post;
        }

        @Override
        protected Class<Post> entity() {
            return Post.class;
        }

        @Override
        protected Table<PostRecord> table() {
            return Tables.POST;
        }

        @Override
        protected TableField<PostRecord, Long> idField() {
            return Tables.POST.ID;
        }

        @Override
        protected BiFunction<RecordMapper<PostRecord>, Post, RecordMapper<PostRecord>> mapper() {
            return (mapper, post) -> mapper
                    .set(Tables.POST.ID, post.id())
                    .set(Tables.POST.OID, post.oid())
                    .set(Tables.POST.URL, post.url())
                    .set(Tables.POST.AUTHOR_ID, post.authorId())
                    .set(Tables.POST.TOPIC_ID, post.topicId())
                    .set(Tables.POST.CONTENT, post.content())
                    .set(Tables.POST.CREATED_AT, of(post.createdAt()))
                    .set(Tables.POST.LAST_UPDATE, of(post.lastUpdate()));
        }

        @Override
        protected Function<Post, Long> idMapper() {
            return Post::id;
        }
    }
}
