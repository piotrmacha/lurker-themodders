/*
 * This file is generated by jOOQ.
 */
package pl.piotrmacha.lurker.jooq.tables.records;


import org.jooq.Record1;
import org.jooq.impl.UpdatableRecordImpl;

import pl.piotrmacha.lurker.jooq.tables.Category;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class CategoryRecord extends UpdatableRecordImpl<CategoryRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>public.category.id</code>.
     */
    public void setId(String value) {
        set(0, value);
    }

    /**
     * Getter for <code>public.category.id</code>.
     */
    public String getId() {
        return (String) get(0);
    }

    /**
     * Setter for <code>public.category.name</code>.
     */
    public void setName(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>public.category.name</code>.
     */
    public String getName() {
        return (String) get(1);
    }

    /**
     * Setter for <code>public.category.url</code>.
     */
    public void setUrl(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>public.category.url</code>.
     */
    public String getUrl() {
        return (String) get(2);
    }

    /**
     * Setter for <code>public.category.description</code>.
     */
    public void setDescription(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>public.category.description</code>.
     */
    public String getDescription() {
        return (String) get(3);
    }

    /**
     * Setter for <code>public.category.parent</code>.
     */
    public void setParent(String value) {
        set(4, value);
    }

    /**
     * Getter for <code>public.category.parent</code>.
     */
    public String getParent() {
        return (String) get(4);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<String> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached CategoryRecord
     */
    public CategoryRecord() {
        super(Category.CATEGORY);
    }

    /**
     * Create a detached, initialised CategoryRecord
     */
    public CategoryRecord(String id, String name, String url, String description, String parent) {
        super(Category.CATEGORY);

        setId(id);
        setName(name);
        setUrl(url);
        setDescription(description);
        setParent(parent);
        resetChangedOnNotNull();
    }
}
