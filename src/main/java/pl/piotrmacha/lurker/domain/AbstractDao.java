package pl.piotrmacha.lurker.domain;

import lombok.RequiredArgsConstructor;
import org.jooq.*;
import org.jooq.Record;

import java.time.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

@RequiredArgsConstructor
public abstract class AbstractDao<T, R extends Record, ID> {
    protected final DSLContext context;

    protected abstract Class<T> entity();

    protected abstract Table<R> table();

    protected abstract TableField<R, ID> idField();

    protected abstract BiFunction<RecordMapper<R>, T, RecordMapper<R>> mapper();

    protected abstract Function<T, ID> idMapper();

    public T save(T entity) {
        return idMapper().apply(entity) == null
                ? create(entity)
                : (find(idMapper().apply(entity)).isEmpty() ? create(entity) : update(entity));
    }

    public Optional<T> find(ID id) {
        return Optional.ofNullable(context.selectFrom(table())
                .where(idField().eq(id))
                .fetchOneInto(entity()));
    }

    public Optional<T> findBy(Function<SelectWhereStep<R>, SelectConditionStep<R>> mapper) {
        return Optional.ofNullable(mapper.apply(context.selectFrom(table()))
                .fetchOneInto(entity()));
    }

    public Optional<T> findForUpdate(ID id) {
        return Optional.ofNullable(context.selectFrom(table())
                .where(idField().eq(id))
                .forUpdate()
                .fetchOneInto(entity()));
    }

    public Optional<T> findByForUpdate(Function<SelectWhereStep<R>, SelectConditionStep<R>> mapper) {
        return Optional.ofNullable(mapper.apply(context.selectFrom(table()))
                .forUpdate()
                .fetchOneInto(entity()));
    }

    public T get(ID id) {
        return find(id).orElseThrow(() -> new RuntimeException("Entity not found"));
    }

    public T getBy(Function<SelectWhereStep<R>, SelectConditionStep<R>> mapper) {
        return findBy(mapper).orElseThrow(() -> new RuntimeException("Entity not found"));
    }

    public T getForUpdate(ID id) {
        return findForUpdate(id).orElseThrow(() -> new RuntimeException("Entity not found"));
    }

    public T getByForUpdate(Function<SelectWhereStep<R>, SelectConditionStep<R>> mapper) {
        return findByForUpdate(mapper).orElseThrow(() -> new RuntimeException("Entity not found"));
    }

    public Stream<T> findAll() {
        return context.selectFrom(table()).stream().map(record -> record.into(entity()));
    }

    public Stream<T> findAll(Function<SelectWhereStep<R>, SelectConditionStep<R>> mapper) {
        return mapper.apply(context.selectFrom(table())).stream().map(record -> record.into(entity()));
    }

    public T create(T entity) {
        RecordMapper<R> recordMapper = mapper().apply(new RecordMapper<>(), entity);
        return context.insertInto(table())
                .set(recordMapper.getMapping())
                .returning()
                .fetchOneInto(entity());
    }

    public T update(T entity) {
        RecordMapper<R> recordMapper = mapper().apply(new RecordMapper<>(), entity);
        return context.update(table())
                .set(recordMapper.getMapping())
                .where(idField().eq(idMapper().apply(entity)))
                .returning()
                .fetchOneInto(entity());
    }

    public void delete(T entity) {
        context.deleteFrom(table())
                .where(idField().eq(idMapper().apply(entity)))
                .execute();
    }

    public void deleteById(ID id) {
        context.deleteFrom(table())
                .where(idField().eq(id))
                .execute();
    }

    public void deleteAll() {
        context.deleteFrom(table()).execute();
    }

    public void deleteAll(Function<DeleteWhereStep<R>, DeleteConditionStep<R>> mapper) {
        mapper.apply(context.deleteFrom(table())).execute();
    }

    protected OffsetDateTime of(Instant instant) {
        if (instant == null) {
            return null;
        }
        ZonedDateTime zonedDateTime = instant.atZone(ZoneOffset.systemDefault());
        return OffsetDateTime.of(zonedDateTime.toLocalDateTime(), zonedDateTime.getOffset());
    }

    @RequiredArgsConstructor
    protected static class RecordMapper<R extends Record> {
        private final Map<TableField<R, ?>, Object> fieldMapping = new HashMap<>();

        public <S> RecordMapper<R> set(TableField<R, S> field, S value) {
            fieldMapping.put(field, value);
            return this;
        }

        private Map<TableField<R, ?>, Object> getMapping() {
            return fieldMapping;
        }
    }
}
