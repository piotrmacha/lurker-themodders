/*
 * This file is generated by jOOQ.
 */
package pl.piotrmacha.lurker.jooq.tables.records;


import java.time.OffsetDateTime;

import org.jooq.Record1;
import org.jooq.impl.UpdatableRecordImpl;

import pl.piotrmacha.lurker.jooq.tables.DownloadQueueFailure;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class DownloadQueueFailureRecord extends UpdatableRecordImpl<DownloadQueueFailureRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>public.download_queue_failure.id</code>.
     */
    public void setId(Long value) {
        set(0, value);
    }

    /**
     * Getter for <code>public.download_queue_failure.id</code>.
     */
    public Long getId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>public.download_queue_failure.type</code>.
     */
    public void setType(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>public.download_queue_failure.type</code>.
     */
    public String getType() {
        return (String) get(1);
    }

    /**
     * Setter for <code>public.download_queue_failure.url</code>.
     */
    public void setUrl(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>public.download_queue_failure.url</code>.
     */
    public String getUrl() {
        return (String) get(2);
    }

    /**
     * Setter for <code>public.download_queue_failure.entity_id</code>.
     */
    public void setEntityId(Long value) {
        set(3, value);
    }

    /**
     * Getter for <code>public.download_queue_failure.entity_id</code>.
     */
    public Long getEntityId() {
        return (Long) get(3);
    }

    /**
     * Setter for <code>public.download_queue_failure.error</code>.
     */
    public void setError(String value) {
        set(4, value);
    }

    /**
     * Getter for <code>public.download_queue_failure.error</code>.
     */
    public String getError() {
        return (String) get(4);
    }

    /**
     * Setter for <code>public.download_queue_failure.message</code>.
     */
    public void setMessage(String value) {
        set(5, value);
    }

    /**
     * Getter for <code>public.download_queue_failure.message</code>.
     */
    public String getMessage() {
        return (String) get(5);
    }

    /**
     * Setter for <code>public.download_queue_failure.created_at</code>.
     */
    public void setCreatedAt(OffsetDateTime value) {
        set(6, value);
    }

    /**
     * Getter for <code>public.download_queue_failure.created_at</code>.
     */
    public OffsetDateTime getCreatedAt() {
        return (OffsetDateTime) get(6);
    }

    /**
     * Setter for <code>public.download_queue_failure.failed_at</code>.
     */
    public void setFailedAt(OffsetDateTime value) {
        set(7, value);
    }

    /**
     * Getter for <code>public.download_queue_failure.failed_at</code>.
     */
    public OffsetDateTime getFailedAt() {
        return (OffsetDateTime) get(7);
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
     * Create a detached DownloadQueueFailureRecord
     */
    public DownloadQueueFailureRecord() {
        super(DownloadQueueFailure.DOWNLOAD_QUEUE_FAILURE);
    }

    /**
     * Create a detached, initialised DownloadQueueFailureRecord
     */
    public DownloadQueueFailureRecord(Long id, String type, String url, Long entityId, String error, String message, OffsetDateTime createdAt, OffsetDateTime failedAt) {
        super(DownloadQueueFailure.DOWNLOAD_QUEUE_FAILURE);

        setId(id);
        setType(type);
        setUrl(url);
        setEntityId(entityId);
        setError(error);
        setMessage(message);
        setCreatedAt(createdAt);
        setFailedAt(failedAt);
        resetChangedOnNotNull();
    }
}
