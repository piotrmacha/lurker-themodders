package pl.piotrmacha.lurker.domain;

import lombok.RequiredArgsConstructor;
import lombok.With;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.exception.NoDataFoundException;
import org.springframework.stereotype.Component;
import pl.piotrmacha.lurker.jooq.Tables;
import pl.piotrmacha.lurker.jooq.tables.records.DownloadQueueDoneRecord;
import pl.piotrmacha.lurker.jooq.tables.records.DownloadQueueFailureRecord;
import pl.piotrmacha.lurker.jooq.tables.records.DownloadQueueRecord;
import pl.piotrmacha.lurker.jooq.tables.records.DownloadQueueScheduledRecord;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.jooq.impl.DSL.count;

@Slf4j
@Component
@RequiredArgsConstructor
public class DownloadQueue {
    public Optional<Task> poll() {
        return poll(Set.of(TaskType.ASSET, TaskType.BOARD, TaskType.TOPIC));
    }

    public Optional<Task> poll(Set<TaskType> types) {
        return Task.dao().pollNext(types);
    }

    public int size() {
        return size(Set.of(TaskType.ASSET, TaskType.BOARD, TaskType.TOPIC));
    }

    public int size(Set<TaskType> types) {
        return Task.dao().queueSize(types);
    }

    public void enqueue(TaskType type, PageInfo.Uri page, Long entityId) {
        String url = type.normalize(page).uri().toString();
        if (wasUrlAlreadyEnqueued(url)) {
            log.info("Task already enqueued, url: {}", url);
            return;
        }
        log.info("Enqueueing task {}, url: {}", type, url);
        Task.of(type, url, entityId).save();
    }

    private static boolean wasUrlAlreadyEnqueued(String url) {
        return Task.dao().countByUrl(url) > 0 || TaskDone.dao().countByUrlAndDate(url, OffsetDateTime.now().minusHours(1)) > 0;
    }

    public enum TaskType {
        ASSET,
        BOARD,
        TOPIC;

        PageInfo normalize(PageInfo.Uri uri) {
            return switch (this) {
                case ASSET -> uri.normalize();
                case BOARD -> uri.asBoard().normalizeOffset();
                case TOPIC -> uri.asTopic().normalizeOffset();
            };
        }
    }

    @With
    public record Task(
            Long id,
            TaskType type,
            String url,
            Long entityId,
            Instant lockedAt,
            Instant createdAt
    ) {
        public TaskDone done() {
            TaskDone taskDone = TaskDone.dao().save(new TaskDone(id, type, url, entityId, createdAt, Instant.now()));
            dao().deleteById(id);
            return taskDone;
        }

        public TaskFailure failure(String error, String message) {
            TaskFailure taskFailure = TaskFailure.dao().save(new TaskFailure(id, type, url, entityId, error, message, createdAt, Instant.now()));
            dao().deleteById(id);
            return taskFailure;
        }

        public static Task of(Long id, TaskType type, String url, Long entityId, Instant lockedAt, Instant createdAt) {
            return new Task(id, type, url, entityId, lockedAt, createdAt);
        }

        public static Task of(TaskType type, String url, Long entityId) {
            return of(null, type, url, entityId, null, Instant.now());
        }

        private static TaskDao dao;

        public static TaskDao dao() {
            return dao;
        }

        public Task save() {
            return dao().save(this);
        }

        @Component
        public static class TaskDao extends AbstractDao<Task, DownloadQueueRecord, Long> {
            public TaskDao(DSLContext context) {
                super(context);
                dao = this;
            }

            int queueSize(Set<TaskType> types) {
                return context.select(count(Tables.DOWNLOAD_QUEUE.ID))
                        .from(Tables.DOWNLOAD_QUEUE)
                        .where(Tables.DOWNLOAD_QUEUE.TYPE.in(types.stream().map(Enum::name).toList()))
                        .fetchSingleInto(Integer.class);
            }

            Optional<Task> pollNext(Set<TaskType> types) {
                try {
                    Task task = context.update(Tables.DOWNLOAD_QUEUE)
                            .set(Tables.DOWNLOAD_QUEUE.LOCKED_AT, of(Instant.now()))
                            .where(
                                    Tables.DOWNLOAD_QUEUE.TYPE.in(types.stream().map(Enum::name).toList()),
                                    Tables.DOWNLOAD_QUEUE.LOCKED_AT.isNull()
                            )
                            .orderBy(Tables.DOWNLOAD_QUEUE.CREATED_AT.asc())
                            .limit(1)
                            .returning()
                            .fetchSingleInto(Task.class);
                    return Optional.ofNullable(task);
                } catch (NoDataFoundException e) {
                    return Optional.empty();
                }
            }

            public int countByUrl(String url) {
                return context.select(count(Tables.DOWNLOAD_QUEUE.ID))
                        .from(Tables.DOWNLOAD_QUEUE)
                        .where(Tables.DOWNLOAD_QUEUE.URL.eq(url))
                        .fetchSingleInto(Integer.class);
            }

            @Override
            public Task save(Task entity) {
                if (entity.id() == null) {
                    return super.create(entity.withId(context.nextval("download_queue_id_seq").longValue()));
                }
                return super.save(entity);
            }

            @Override
            protected Class<Task> entity() {
                return Task.class;
            }

            @Override
            protected Table<DownloadQueueRecord> table() {
                return Tables.DOWNLOAD_QUEUE;
            }

            @Override
            protected TableField<DownloadQueueRecord, Long> idField() {
                return Tables.DOWNLOAD_QUEUE.ID;
            }

            @Override
            protected BiFunction<RecordMapper<DownloadQueueRecord>, Task, RecordMapper<DownloadQueueRecord>> mapper() {
                return (mapper, taks) -> mapper
                        .set(Tables.DOWNLOAD_QUEUE.ID, taks.id())
                        .set(Tables.DOWNLOAD_QUEUE.TYPE, taks.type().name())
                        .set(Tables.DOWNLOAD_QUEUE.URL, taks.url())
                        .set(Tables.DOWNLOAD_QUEUE.ENTITY_ID, taks.entityId())
                        .set(Tables.DOWNLOAD_QUEUE.LOCKED_AT, taks.lockedAt() != null ? of(taks.lockedAt()) : null)
                        .set(Tables.DOWNLOAD_QUEUE.CREATED_AT, of(taks.createdAt()));
            }

            @Override
            protected Function<Task, Long> idMapper() {
                return Task::id;
            }
        }
    }

    @With
    public record TaskDone(
            Long id,
            TaskType type,
            String url,
            Long entityId,
            Instant createdAt,
            Instant doneAt
    ) {
        private static TaskDoneDao dao;

        public static TaskDoneDao dao() {
            return dao;
        }

        @Component
        public static class TaskDoneDao extends AbstractDao<TaskDone, DownloadQueueDoneRecord, Long> {
            public TaskDoneDao(DSLContext context) {
                super(context);
                dao = this;
            }

            public int countByUrlAndDate(String url, OffsetDateTime date) {
                return context.select(count(Tables.DOWNLOAD_QUEUE_DONE.ID))
                        .from(Tables.DOWNLOAD_QUEUE_DONE)
                        .where(
                                Tables.DOWNLOAD_QUEUE_DONE.URL.eq(url),
                                Tables.DOWNLOAD_QUEUE_DONE.DONE_AT.le(date)
                        )
                        .fetchSingleInto(Integer.class);
            }

            @Override
            protected Class<TaskDone> entity() {
                return TaskDone.class;
            }

            @Override
            protected Table<DownloadQueueDoneRecord> table() {
                return Tables.DOWNLOAD_QUEUE_DONE;
            }

            @Override
            protected TableField<DownloadQueueDoneRecord, Long> idField() {
                return Tables.DOWNLOAD_QUEUE_DONE.ID;
            }

            @Override
            protected BiFunction<RecordMapper<DownloadQueueDoneRecord>, TaskDone, RecordMapper<DownloadQueueDoneRecord>> mapper() {
                return (mapper, task) -> mapper
                        .set(Tables.DOWNLOAD_QUEUE_DONE.ID, task.id())
                        .set(Tables.DOWNLOAD_QUEUE_DONE.TYPE, task.type().name())
                        .set(Tables.DOWNLOAD_QUEUE_DONE.URL, task.url())
                        .set(Tables.DOWNLOAD_QUEUE_DONE.ENTITY_ID, task.entityId())
                        .set(Tables.DOWNLOAD_QUEUE_DONE.CREATED_AT, of(task.createdAt()))
                        .set(Tables.DOWNLOAD_QUEUE_DONE.DONE_AT, of(task.doneAt()));
            }

            @Override
            protected Function<TaskDone, Long> idMapper() {
                return TaskDone::id;
            }
        }
    }

    @With
    public record TaskFailure(
            Long id,
            TaskType type,
            String url,
            Long entityId,
            String error,
            String message,
            Instant createdAt,
            Instant failedAt
    ) {
        private static TaskFailureDao dao;

        public static TaskFailureDao dao() {
            return dao;
        }

        @Component
        public static class TaskFailureDao extends AbstractDao<TaskFailure, DownloadQueueFailureRecord, Long> {
            public TaskFailureDao(DSLContext context) {
                super(context);
                dao = this;
            }

            @Override
            protected Class<TaskFailure> entity() {
                return TaskFailure.class;
            }

            @Override
            protected Table<DownloadQueueFailureRecord> table() {
                return Tables.DOWNLOAD_QUEUE_FAILURE;
            }

            @Override
            protected TableField<DownloadQueueFailureRecord, Long> idField() {
                return Tables.DOWNLOAD_QUEUE_FAILURE.ID;
            }

            @Override
            protected BiFunction<RecordMapper<DownloadQueueFailureRecord>, TaskFailure, RecordMapper<DownloadQueueFailureRecord>> mapper() {
                return (mapper, taks) -> mapper
                        .set(Tables.DOWNLOAD_QUEUE_FAILURE.ID, taks.id())
                        .set(Tables.DOWNLOAD_QUEUE_FAILURE.TYPE, taks.type().name())
                        .set(Tables.DOWNLOAD_QUEUE_FAILURE.URL, taks.url())
                        .set(Tables.DOWNLOAD_QUEUE_FAILURE.ENTITY_ID, taks.entityId())
                        .set(Tables.DOWNLOAD_QUEUE_FAILURE.ERROR, taks.error())
                        .set(Tables.DOWNLOAD_QUEUE_FAILURE.MESSAGE, taks.message())
                        .set(Tables.DOWNLOAD_QUEUE_FAILURE.CREATED_AT, of(taks.createdAt()))
                        .set(Tables.DOWNLOAD_QUEUE_FAILURE.FAILED_AT, of(taks.failedAt()));
            }

            @Override
            protected Function<TaskFailure, Long> idMapper() {
                return TaskFailure::id;
            }
        }
    }

    @With
    public record TaskScheduled(
            Long id,
            TaskType type,
            String url,
            Long entityId,
            Instant createdAt,
            Instant scheduledAt
    ) {
        private static TaskScheduledDao dao;

        public static TaskScheduledDao dao() {
            return dao;
        }

        @Component
        public static class TaskScheduledDao extends AbstractDao<TaskScheduled, DownloadQueueScheduledRecord, Long> {
            public TaskScheduledDao(DSLContext context) {
                super(context);
                dao = this;
            }

            @Override
            public TaskScheduled save(TaskScheduled entity) {
                if (entity.id() == null) {
                    return super.create(entity.withId(context.nextval("download_queue_scheduled_id_seq").longValue()));
                }
                return super.save(entity);
            }

            @Override
            protected Class<TaskScheduled> entity() {
                return TaskScheduled.class;
            }

            @Override
            protected Table<DownloadQueueScheduledRecord> table() {
                return Tables.DOWNLOAD_QUEUE_SCHEDULED;
            }

            @Override
            protected TableField<DownloadQueueScheduledRecord, Long> idField() {
                return Tables.DOWNLOAD_QUEUE_SCHEDULED.ID;
            }

            @Override
            protected BiFunction<RecordMapper<DownloadQueueScheduledRecord>, TaskScheduled, RecordMapper<DownloadQueueScheduledRecord>> mapper() {
                return (mapper, taks) -> mapper
                        .set(Tables.DOWNLOAD_QUEUE_SCHEDULED.ID, taks.id())
                        .set(Tables.DOWNLOAD_QUEUE_SCHEDULED.TYPE, taks.type().name())
                        .set(Tables.DOWNLOAD_QUEUE_SCHEDULED.URL, taks.url())
                        .set(Tables.DOWNLOAD_QUEUE_SCHEDULED.ENTITY_ID, taks.entityId())
                        .set(Tables.DOWNLOAD_QUEUE_SCHEDULED.CREATED_AT, of(taks.createdAt()))
                        .set(Tables.DOWNLOAD_QUEUE_SCHEDULED.SCHEDULED_AT, of(taks.scheduledAt()));
            }

            @Override
            protected Function<TaskScheduled, Long> idMapper() {
                return TaskScheduled::id;
            }
        }
    }
}
