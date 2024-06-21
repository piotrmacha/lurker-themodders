/*
 * This file is generated by jOOQ.
 */
package pl.piotrmacha.lurker.jooq.tables;


import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jooq.Check;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.PlainSQL;
import org.jooq.QueryPart;
import org.jooq.SQL;
import org.jooq.Schema;
import org.jooq.Select;
import org.jooq.Stringly;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

import pl.piotrmacha.lurker.jooq.Keys;
import pl.piotrmacha.lurker.jooq.Public;
import pl.piotrmacha.lurker.jooq.tables.records.DownloadQueueFailureRecord;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class DownloadQueueFailure extends TableImpl<DownloadQueueFailureRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>public.download_queue_failure</code>
     */
    public static final DownloadQueueFailure DOWNLOAD_QUEUE_FAILURE = new DownloadQueueFailure();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<DownloadQueueFailureRecord> getRecordType() {
        return DownloadQueueFailureRecord.class;
    }

    /**
     * The column <code>public.download_queue_failure.id</code>.
     */
    public final TableField<DownloadQueueFailureRecord, Long> ID = createField(DSL.name("id"), SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>public.download_queue_failure.type</code>.
     */
    public final TableField<DownloadQueueFailureRecord, String> TYPE = createField(DSL.name("type"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>public.download_queue_failure.url</code>.
     */
    public final TableField<DownloadQueueFailureRecord, String> URL = createField(DSL.name("url"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>public.download_queue_failure.entity_id</code>.
     */
    public final TableField<DownloadQueueFailureRecord, Long> ENTITY_ID = createField(DSL.name("entity_id"), SQLDataType.BIGINT, this, "");

    /**
     * The column <code>public.download_queue_failure.error</code>.
     */
    public final TableField<DownloadQueueFailureRecord, String> ERROR = createField(DSL.name("error"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>public.download_queue_failure.message</code>.
     */
    public final TableField<DownloadQueueFailureRecord, String> MESSAGE = createField(DSL.name("message"), SQLDataType.CLOB, this, "");

    /**
     * The column <code>public.download_queue_failure.created_at</code>.
     */
    public final TableField<DownloadQueueFailureRecord, OffsetDateTime> CREATED_AT = createField(DSL.name("created_at"), SQLDataType.TIMESTAMPWITHTIMEZONE(6).nullable(false).defaultValue(DSL.field(DSL.raw("now()"), SQLDataType.TIMESTAMPWITHTIMEZONE)), this, "");

    /**
     * The column <code>public.download_queue_failure.failed_at</code>.
     */
    public final TableField<DownloadQueueFailureRecord, OffsetDateTime> FAILED_AT = createField(DSL.name("failed_at"), SQLDataType.TIMESTAMPWITHTIMEZONE(6).nullable(false).defaultValue(DSL.field(DSL.raw("now()"), SQLDataType.TIMESTAMPWITHTIMEZONE)), this, "");

    private DownloadQueueFailure(Name alias, Table<DownloadQueueFailureRecord> aliased) {
        this(alias, aliased, (Field<?>[]) null, null);
    }

    private DownloadQueueFailure(Name alias, Table<DownloadQueueFailureRecord> aliased, Field<?>[] parameters, Condition where) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table(), where);
    }

    /**
     * Create an aliased <code>public.download_queue_failure</code> table
     * reference
     */
    public DownloadQueueFailure(String alias) {
        this(DSL.name(alias), DOWNLOAD_QUEUE_FAILURE);
    }

    /**
     * Create an aliased <code>public.download_queue_failure</code> table
     * reference
     */
    public DownloadQueueFailure(Name alias) {
        this(alias, DOWNLOAD_QUEUE_FAILURE);
    }

    /**
     * Create a <code>public.download_queue_failure</code> table reference
     */
    public DownloadQueueFailure() {
        this(DSL.name("download_queue_failure"), null);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Public.PUBLIC;
    }

    @Override
    public UniqueKey<DownloadQueueFailureRecord> getPrimaryKey() {
        return Keys.DOWNLOAD_QUEUE_FAILURE_PKEY;
    }

    @Override
    public List<Check<DownloadQueueFailureRecord>> getChecks() {
        return Arrays.asList(
            Internal.createCheck(this, DSL.name("download_queue_failure_type_check"), "((type = ANY (ARRAY['ASSET'::text, 'BOARD'::text, 'TOPIC'::text])))", true)
        );
    }

    @Override
    public DownloadQueueFailure as(String alias) {
        return new DownloadQueueFailure(DSL.name(alias), this);
    }

    @Override
    public DownloadQueueFailure as(Name alias) {
        return new DownloadQueueFailure(alias, this);
    }

    @Override
    public DownloadQueueFailure as(Table<?> alias) {
        return new DownloadQueueFailure(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public DownloadQueueFailure rename(String name) {
        return new DownloadQueueFailure(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public DownloadQueueFailure rename(Name name) {
        return new DownloadQueueFailure(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public DownloadQueueFailure rename(Table<?> name) {
        return new DownloadQueueFailure(name.getQualifiedName(), null);
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public DownloadQueueFailure where(Condition condition) {
        return new DownloadQueueFailure(getQualifiedName(), aliased() ? this : null, null, condition);
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public DownloadQueueFailure where(Collection<? extends Condition> conditions) {
        return where(DSL.and(conditions));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public DownloadQueueFailure where(Condition... conditions) {
        return where(DSL.and(conditions));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public DownloadQueueFailure where(Field<Boolean> condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public DownloadQueueFailure where(SQL condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public DownloadQueueFailure where(@Stringly.SQL String condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public DownloadQueueFailure where(@Stringly.SQL String condition, Object... binds) {
        return where(DSL.condition(condition, binds));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public DownloadQueueFailure where(@Stringly.SQL String condition, QueryPart... parts) {
        return where(DSL.condition(condition, parts));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public DownloadQueueFailure whereExists(Select<?> select) {
        return where(DSL.exists(select));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public DownloadQueueFailure whereNotExists(Select<?> select) {
        return where(DSL.notExists(select));
    }
}
