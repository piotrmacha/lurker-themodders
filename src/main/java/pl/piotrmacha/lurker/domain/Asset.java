package pl.piotrmacha.lurker.domain;

import com.fasterxml.jackson.databind.introspect.AnnotationCollector;
import lombok.With;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.exception.NoDataFoundException;
import org.springframework.stereotype.Component;
import pl.piotrmacha.lurker.jooq.Tables;
import pl.piotrmacha.lurker.jooq.tables.records.AssetRecord;

import java.time.Instant;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

@With
public record Asset(
        Long id,
        String name,
        String url,
        String path,
        String mimeType,
        Long size,
        Instant lastUpdate
) {
    private static AssetDao dao;

    public Asset save() {
        return dao.save(this.withLastUpdate(Instant.now()));
    }

    public Asset withDownloadInfo(String path, String mimeType, Long size) {
        return withPath(path)
                .withMimeType(mimeType)
                .withSize(size)
                .withLastUpdate(Instant.now());
    }

    public static AssetDao dao() {
        return dao;
    }

    public static Asset of(Long id, String name, String url, String path, String mimeType, Long size, Instant lastUpdate) {
        return new Asset(id, name, url, path, mimeType, size, lastUpdate);
    }

    public static Asset of(String name, String url, String path, String mimeType, Long size) {
        return of(null, name, url, path, mimeType, size, Instant.now());
    }

    public static Asset of(String name, String url) {
        return of(name, url, null, null, null);
    }

    @Component
    public static class AssetDao extends AbstractDao<Asset, AssetRecord, Long> {
        public AssetDao(DSLContext context) {
            super(context);
            Asset.dao = this;
        }

        public Optional<Asset> findByUrl(String url) {
            try {
                return Optional.of(context.selectFrom(Tables.ASSET)
                        .where(Tables.ASSET.URL.eq(url))
                        .fetchSingleInto(Asset.class));
            } catch (NoDataFoundException e) {
                return Optional.empty();
            }
        }

        @Override
        public Asset create(Asset entity) {
            if (entity.id() == null) {
                return super.create(entity.withId(context.nextval("asset_id_seq").longValue()));
            }
            return super.create(entity);
        }

        @Override
        protected Class<Asset> entity() {
            return Asset.class;
        }

        @Override
        protected Table<AssetRecord> table() {
            return Tables.ASSET;
        }

        @Override
        protected TableField<AssetRecord, Long> idField() {
            return Tables.ASSET.ID;
        }

        @Override
        protected BiFunction<RecordMapper<AssetRecord>, Asset, RecordMapper<AssetRecord>> mapper() {
            return (mapper, asset) -> mapper
                    .set(Tables.ASSET.ID, asset.id())
                    .set(Tables.ASSET.NAME, asset.name())
                    .set(Tables.ASSET.URL, asset.url())
                    .set(Tables.ASSET.PATH, asset.path())
                    .set(Tables.ASSET.MIME_TYPE, asset.mimeType())
                    .set(Tables.ASSET.SIZE, asset.size());
        }

        @Override
        protected Function<Asset, Long> idMapper() {
            return Asset::id;
        }
    }
}
