/*
 * This file is generated by jOOQ.
 */
package pl.piotrmacha.lurker.jooq.tables.records;


import org.jooq.Record1;
import org.jooq.impl.UpdatableRecordImpl;

import pl.piotrmacha.lurker.jooq.tables.Account;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class AccountRecord extends UpdatableRecordImpl<AccountRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>public.account.id</code>.
     */
    public void setId(String value) {
        set(0, value);
    }

    /**
     * Getter for <code>public.account.id</code>.
     */
    public String getId() {
        return (String) get(0);
    }

    /**
     * Setter for <code>public.account.username</code>.
     */
    public void setUsername(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>public.account.username</code>.
     */
    public String getUsername() {
        return (String) get(1);
    }

    /**
     * Setter for <code>public.account.url</code>.
     */
    public void setUrl(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>public.account.url</code>.
     */
    public String getUrl() {
        return (String) get(2);
    }

    /**
     * Setter for <code>public.account.avatar</code>.
     */
    public void setAvatar(Long value) {
        set(3, value);
    }

    /**
     * Getter for <code>public.account.avatar</code>.
     */
    public Long getAvatar() {
        return (Long) get(3);
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
     * Create a detached AccountRecord
     */
    public AccountRecord() {
        super(Account.ACCOUNT);
    }

    /**
     * Create a detached, initialised AccountRecord
     */
    public AccountRecord(String id, String username, String url, Long avatar) {
        super(Account.ACCOUNT);

        setId(id);
        setUsername(username);
        setUrl(url);
        setAvatar(avatar);
        resetChangedOnNotNull();
    }
}