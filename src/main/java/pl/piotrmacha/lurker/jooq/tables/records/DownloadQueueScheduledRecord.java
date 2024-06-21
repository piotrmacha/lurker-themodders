/*
 * This file is generated by jOOQ.
 */
package pl.piotrmacha.lurker.jooq.tables.records;


import java.time.OffsetDateTime;

import org.jooq.Record1;
import org.jooq.impl.UpdatableRecordImpl;

import pl.piotrmacha.lurker.jooq.tables.DownloadQueueScheduled;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class DownloadQueueScheduledRecord extends UpdatableRecordImpl<DownloadQueueScheduledRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>public.download_queue_scheduled.id</code>.
     */
    public void setId(Long value) {
        set(0, value);
    }

    /**
     * Getter for <code>public.download_queue_scheduled.id</code>.
     */
    public Long getId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>public.download_queue_scheduled.type</code>.
     */
    public void setType(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>public.download_queue_scheduled.type</code>.
     */
    public String getType() {
        return (String) get(1);
    }

    /**
     * Setter for <code>public.download_queue_scheduled.url</code>.
     */
    public void setUrl(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>public.download_queue_scheduled.url</code>.
     */
    public String getUrl() {
        return (String) get(2);
    }

    /**
     * Setter for <code>public.download_queue_scheduled.entity_id</code>.
     */
    public void setEntityId(Long value) {
        set(3, value);
    }

    /**
     * Getter for <code>public.download_queue_scheduled.entity_id</code>.
     */
    public Long getEntityId() {
        return (Long) get(3);
    }

    /**
     * Setter for <code>public.download_queue_scheduled.created_at</code>.
     */
    public void setCreatedAt(OffsetDateTime value) {
        set(4, value);
    }

    /**
     * Getter for <code>public.download_queue_scheduled.created_at</code>.
     */
    public OffsetDateTime getCreatedAt() {
        return (OffsetDateTime) get(4);
    }

    /**
     * Setter for <code>public.download_queue_scheduled.scheduled_at</code>.
     */
    public void setScheduledAt(OffsetDateTime value) {
        set(5, value);
    }

    /**
     * Getter for <code>public.download_queue_scheduled.scheduled_at</code>.
     */
    public OffsetDateTime getScheduledAt() {
        return (OffsetDateTime) get(5);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Long> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached DownloadQueueScheduledRecord
     */
    public DownloadQueueScheduledRecord() {
        super(DownloadQueueScheduled.DOWNLOAD_QUEUE_SCHEDULED);
    }

    /**
     * Create a detached, initialised DownloadQueueScheduledRecord
     */
    public DownloadQueueScheduledRecord(Long id, String type, String url, Long entityId, OffsetDateTime createdAt, OffsetDateTime scheduledAt) {
        super(DownloadQueueScheduled.DOWNLOAD_QUEUE_SCHEDULED);

        setId(id);
        setType(type);
        setUrl(url);
        setEntityId(entityId);
        setCreatedAt(createdAt);
        setScheduledAt(scheduledAt);
        resetChangedOnNotNull();
    }
}
