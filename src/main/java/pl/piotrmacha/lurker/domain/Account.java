package pl.piotrmacha.lurker.domain;

import lombok.With;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;
import org.springframework.stereotype.Component;
import pl.piotrmacha.lurker.jooq.Tables;
import pl.piotrmacha.lurker.jooq.tables.records.AccountRecord;

import java.time.Instant;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

@With
public record Account(
        Long id,
        String oid,
        String url,
        String username,
        Long avatarId,
        Instant lastUpdate
) {
    private static AccountDao dao;

    public Account save() {
        return dao.save(this.withLastUpdate(Instant.now()));
    }

    public static AccountDao dao() {
        return dao;
    }

    public static Account of(Long id, String oid, String url, String username, Long avatarId, Instant lastUpdate) {
        return new Account(id, oid, url, username, avatarId, lastUpdate);
    }

    public static Account of(String oid, String url, String username, Long avatarId) {
        return of(null, oid, url, username, avatarId, Instant.now());
    }

    public static Account of(String oid, String url, String username) {
        return of(oid, url, username, null);
    }

    @Component
    public static class AccountDao extends AbstractDao<Account, AccountRecord, Long> {
        public AccountDao(DSLContext context) {
            super(context);
            dao = this;
        }

        public Optional<Account> findByOid(String oid) {
            return findBy(s -> s.where(Tables.ACCOUNT.OID.eq(oid)));
        }

        public Account getByOid(String oid) {
            return findByOid(oid).orElseThrow(() -> new RuntimeException("Entity not found"));
        }

        @Override
        public Account create(Account entity) {
            if (entity.id() == null) {
                return super.create(entity.withId(context.nextval("account_id_seq").longValue()));
            }
            return super.create(entity);
        }

        @Override
        protected Class<Account> entity() {
            return Account.class;
        }

        @Override
        protected Table<AccountRecord> table() {
            return Tables.ACCOUNT;
        }

        @Override
        protected TableField<AccountRecord, Long> idField() {
            return Tables.ACCOUNT.ID;
        }

        @Override
        protected BiFunction<RecordMapper<AccountRecord>, Account, RecordMapper<AccountRecord>> mapper() {
            return (mapper, account) -> mapper
                    .set(Tables.ACCOUNT.ID, account.id())
                    .set(Tables.ACCOUNT.OID, account.oid())
                    .set(Tables.ACCOUNT.URL, account.url())
                    .set(Tables.ACCOUNT.USERNAME, account.username())
                    .set(Tables.ACCOUNT.AVATAR_ID, account.avatarId())
                    .set(Tables.ACCOUNT.LAST_UPDATE, of(account.lastUpdate()));
        }

        @Override
        protected Function<Account, Long> idMapper() {
            return Account::id;
        }
    }
}
