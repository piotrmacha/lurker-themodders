package pl.piotrmacha.lurker.domain;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.exception.IntegrityConstraintViolationException;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import pl.piotrmacha.lurker.jooq.Sequences;
import pl.piotrmacha.lurker.jooq.Tables;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static pl.piotrmacha.lurker.domain.DownloadContext.*;

@Component
public class DownloadService {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DownloadService.class);
    private final DSLContext jooq;

    public DownloadService(DSLContext jooq) {
        this.jooq = jooq;
    }

    public void download(DownloadAdapter adapter, String url, String type, Map<String, String> config) {
        log.info("Configuration: {}", config);

        if (config.get(CONFIG_CLEAR_VISITED).equals("true")){
            jooq.delete(Tables.VISITED_URL).execute();
        }

        if (config.get(CONFIG_ASSET_FILESYSTEM).equals("true")) {
            Path assetDir = Path.of(config.get(CONFIG_ASSET_FILESYSTEM_DIR));
            if (Files.notExists(assetDir)) {
                log.info("Creating directory: {}", assetDir);
                try {
                    Files.createDirectories(assetDir);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        DownloadServiceContext context = new DownloadServiceContext(jooq, adapter, config);
        context.enqueueDownload(url, type, "", true);
        context.runAndWaitForFinish();
    }

    static class DownloadServiceContext implements DownloadContext {
        private static final Logger log = org.slf4j.LoggerFactory.getLogger(DownloadServiceContext.class);
        private final DSLContext jooq;
        private final DownloadAdapter adapter;
        private final Map<String, String> config;
        private final ExecutorService executor;
        private final AtomicLong jobCounter = new AtomicLong(0L);
        private final Set<String> downloadedAssets = new HashSet<>();

        public DownloadServiceContext(DSLContext jooq, DownloadAdapter adapter, Map<String, String> config) {
            this.jooq = jooq;
            this.adapter = adapter;
            this.config = config;
            this.executor = Executors.newFixedThreadPool(
                    Integer.parseInt(config.get(CONFIG_THREAD_POOL_THREADS)),
                    config.get(CONFIG_THREAD_POOL_VIRTUAL).equals("true") ? Thread.ofVirtual().factory() : Thread.ofPlatform().factory()
            );
        }

        public void runAndWaitForFinish() {
            Instant start = Instant.now();
            Instant lastCheckpoint = Instant.EPOCH;
            log.info("Starting download");
            while (jobCounter.get() > 0) {
                Instant now = Instant.now();
                if (now.getEpochSecond() - lastCheckpoint.getEpochSecond() > 60) {
                    log.info("Waiting for {} jobs to finish", jobCounter.get());
                    lastCheckpoint = now;
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.warn("Sleep interrupted on a thread waiting for jobs to finish");
                    Thread.yield();
                }
            }
            log.info("All jobs finished in {}", Duration.between(start, Instant.now()));
        }

        @Override
        public Long saveAsset(String name, String url) {
            Result<Record> existing = jooq.select().from(Tables.ASSET)
                    .where(Tables.ASSET.URL.eq(url))
                    .fetch();

            if (existing.isEmpty()) {
                Long id = jooq.nextval(Sequences.ASSET_ID_SEQ);
                try {
                    jooq.insertInto(Tables.ASSET)
                            .set(Tables.ASSET.ID, id)
                            .set(Tables.ASSET.NAME, name)
                            .set(Tables.ASSET.URL, url)
                            .execute();
                    downloadAsset(id, url);
                    return id;
                } catch (IntegrityConstraintViolationException e) {
                    jooq.update(Tables.ASSET)
                            .set(Tables.ASSET.NAME, name)
                            .set(Tables.ASSET.URL, url)
                            .where(Tables.ASSET.ID.eq(id))
                            .execute();
                    downloadAsset(id, url);
                    return id;
                }
            } else {
                if (existing.getFirst().get(Tables.ASSET.DATA) == null) {
                    downloadAsset(existing.getFirst().get(Tables.ASSET.ID), url);
                }
                return existing.getFirst().get(Tables.ASSET.ID);
            }
        }

        @Override
        public String saveAccount(String id, String name, String url, Long avatarId) {
            Result<Record> existing = jooq.select().from(Tables.ACCOUNT)
                    .where(Tables.ACCOUNT.ID.eq(id))
                    .fetch();

            if (existing.isEmpty()) {
                try {
                    jooq.insertInto(Tables.ACCOUNT)
                            .set(Tables.ACCOUNT.ID, id)
                            .set(Tables.ACCOUNT.USERNAME, name)
                            .set(Tables.ACCOUNT.URL, url)
                            .set(Tables.ACCOUNT.AVATAR, avatarId)
                            .execute();
                } catch (IntegrityConstraintViolationException e) {
                    jooq.update(Tables.ACCOUNT)
                            .set(Tables.ACCOUNT.USERNAME, name)
                            .set(Tables.ACCOUNT.URL, url)
                            .set(Tables.ACCOUNT.AVATAR, avatarId)
                            .where(Tables.ACCOUNT.ID.eq(id))
                            .execute();
                    }
            } else {
                jooq.update(Tables.ACCOUNT)
                        .set(Tables.ACCOUNT.USERNAME, name)
                        .set(Tables.ACCOUNT.URL, url)
                        .set(Tables.ACCOUNT.AVATAR, avatarId)
                        .where(Tables.ACCOUNT.ID.eq(id))
                        .execute();
            }

            return id;
        }

        @Override
        public String saveCategory(String id, String name, String url, String description, String parentId) {
            Result<Record> existing = jooq.select().from(Tables.CATEGORY)
                    .where(Tables.CATEGORY.ID.eq(id))
                    .fetch();

            if (existing.isEmpty()) {
                try {
                    jooq.insertInto(Tables.CATEGORY)
                            .set(Tables.CATEGORY.ID, id)
                            .set(Tables.CATEGORY.NAME, name)
                            .set(Tables.CATEGORY.URL, url)
                            .set(Tables.CATEGORY.DESCRIPTION, description)
                            .set(Tables.CATEGORY.PARENT, parentId)
                            .execute();
                } catch (IntegrityConstraintViolationException e) {
                    jooq.update(Tables.CATEGORY)
                            .set(Tables.CATEGORY.NAME, name)
                            .set(Tables.CATEGORY.URL, url)
                            .set(Tables.CATEGORY.DESCRIPTION, description)
                            .set(Tables.CATEGORY.PARENT, parentId)
                            .where(Tables.CATEGORY.ID.eq(id))
                            .execute();
                }
            } else {
                jooq.update(Tables.CATEGORY)
                        .set(Tables.CATEGORY.NAME, name)
                        .set(Tables.CATEGORY.URL, url)
                        .set(Tables.CATEGORY.DESCRIPTION, description)
                        .set(Tables.CATEGORY.PARENT, parentId)
                        .where(Tables.CATEGORY.ID.eq(id))
                        .execute();
            }

            return id;
        }

        @Override
        public String saveThread(String id, String title, String url, String authorId, String categoryId, Instant createdAt) {
            Result<Record> existing = jooq.select().from(Tables.THREAD)
                    .where(Tables.THREAD.ID.eq(id))
                    .fetch();

            ZonedDateTime createdAtZoned = ZonedDateTime.ofInstant(createdAt != null ? createdAt : Instant.EPOCH, java.time.ZoneId.of("Europe/Warsaw"));
            if (existing.isEmpty()) {
                try {
                    jooq.insertInto(Tables.THREAD)
                            .set(Tables.THREAD.ID, id)
                            .set(Tables.THREAD.TITLE, title)
                            .set(Tables.THREAD.URL, url)
                            .set(Tables.THREAD.AUTHOR, authorId)
                            .set(Tables.THREAD.CATEGORY, categoryId)
                            .set(Tables.THREAD.CREATED_AT, OffsetDateTime.from(createdAtZoned))
                            .execute();
                } catch (IntegrityConstraintViolationException e) {
                    jooq.update(Tables.THREAD)
                            .set(Tables.THREAD.TITLE, title)
                            .set(Tables.THREAD.URL, url)
                            .set(Tables.THREAD.AUTHOR, authorId)
                            .set(Tables.THREAD.CATEGORY, categoryId)
                            .set(Tables.THREAD.CREATED_AT, OffsetDateTime.from(createdAtZoned))
                            .where(Tables.THREAD.ID.eq(id))
                            .execute();
                }
            } else {
                jooq.update(Tables.THREAD)
                        .set(Tables.THREAD.TITLE, title)
                        .set(Tables.THREAD.URL, url)
                        .set(Tables.THREAD.AUTHOR, authorId)
                        .set(Tables.THREAD.CATEGORY, categoryId)
                        .set(Tables.THREAD.CREATED_AT, OffsetDateTime.from(createdAtZoned))
                        .where(Tables.THREAD.ID.eq(id))
                        .execute();
            }

            return id;
        }

        @Override
        public String updateThread(String id, String title, String authorId, Instant createdAt) {
            jooq.update(Tables.THREAD)
                    .set(Tables.THREAD.TITLE, title)
                    .set(Tables.THREAD.AUTHOR, authorId)
                    .set(Tables.THREAD.CREATED_AT, OffsetDateTime.from(
                            ZonedDateTime.ofInstant(createdAt != null ? createdAt : Instant.EPOCH, java.time.ZoneId.of("Europe/Warsaw"))
                    ))
                    .where(Tables.THREAD.ID.eq(id))
                    .execute();

            return id;
        }

        @Override
        public String savePost(String id, String content, String url, String authorId, String threadId, Instant createdAt) {
            Result<Record> existing = jooq.select().from(Tables.POST)
                    .where(Tables.POST.ID.eq(id))
                    .fetch();

            ZonedDateTime createdAtZoned = ZonedDateTime.ofInstant(createdAt != null ? createdAt : Instant.EPOCH, java.time.ZoneId.of("Europe/Warsaw"));
            if (existing.isEmpty()) {
                try {
                    jooq.insertInto(Tables.POST)
                            .set(Tables.POST.ID, id)
                            .set(Tables.POST.CONTENT, content)
                            .set(Tables.POST.URL, url)
                            .set(Tables.POST.AUTHOR, authorId)
                            .set(Tables.POST.THREAD, threadId)
                            .set(Tables.POST.CREATED_AT, OffsetDateTime.from(createdAtZoned))
                            .execute();
                } catch (IntegrityConstraintViolationException e) {
                    jooq.update(Tables.POST)
                            .set(Tables.POST.CONTENT, content)
                            .set(Tables.POST.URL, url)
                            .set(Tables.POST.AUTHOR, authorId)
                            .set(Tables.POST.THREAD, threadId)
                            .set(Tables.POST.CREATED_AT, OffsetDateTime.from(createdAtZoned))
                            .where(Tables.POST.ID.eq(id))
                            .execute();
                }
            } else {
                jooq.update(Tables.POST)
                        .set(Tables.POST.CONTENT, content)
                        .set(Tables.POST.URL, url)
                        .set(Tables.POST.AUTHOR, authorId)
                        .set(Tables.POST.THREAD, threadId)
                        .set(Tables.POST.CREATED_AT, OffsetDateTime.from(createdAtZoned))
                        .where(Tables.POST.ID.eq(id))
                        .execute();
            }

            return id;
        }

        @Override
        public boolean hasCategory(String boardId) {
            return jooq.select().from(Tables.CATEGORY)
                    .where(Tables.CATEGORY.ID.eq(boardId))
                    .fetchOptional()
                    .isPresent();
        }

        @Override
        public boolean hasThread(String id) {
            return jooq.select().from(Tables.THREAD)
                    .where(Tables.THREAD.ID.eq(id))
                    .fetchOptional()
                    .isPresent();
        }

        @Override
        public void enqueueDownload(String url, String type, String id) {
            enqueueDownload(url, type, id, false);
        }

        @Override
        public void enqueueDownload(String url, String type, String id, boolean force) {
            if (config.get(CONFIG_SKIP_VISITED).equals("true")) {
                Set<String> visited = jooq.select(Tables.VISITED_URL.URL)
                        .from(Tables.VISITED_URL)
                        .fetchSet(Tables.VISITED_URL.URL);

                if (visited.contains(url) && !force) {
                    log.info("Skipping visited {}", url);
                    return;
                }

                if (visited.contains(url)) {
                    log.info("Forcing download of visited {}", url);
                }
            }

            jobCounter.incrementAndGet();
            executor.submit(() -> {
                try {
                    adapter.download(url, type, id, this, config);
                } catch (Exception e) {
                    log.error("Error downloading {}", url, e);
                } finally {
                    jobCounter.getAndDecrement();
                    if (config.get(CONFIG_SAVE_VISITED).equals("true")) {
                        boolean exists = jooq.select().from(Tables.VISITED_URL).where(Tables.VISITED_URL.URL.eq(url)).fetchOptional().isPresent();
                        if (exists) {
                            jooq.update(Tables.VISITED_URL)
                                    .set(Tables.VISITED_URL.VISITED_AT, OffsetDateTime.now())
                                    .where(Tables.VISITED_URL.URL.eq(url))
                                    .execute();
                        } else {
                            jooq.insertInto(Tables.VISITED_URL)
                                    .set(Tables.VISITED_URL.URL, url)
                                    .set(Tables.VISITED_URL.VISITED_AT, OffsetDateTime.now())
                                    .execute();
                        }
                    }
                }
            });
        }

        private void downloadAsset(Long id, String url) {
            if (downloadedAssets.contains(url)) {
                return;
            }

            jobCounter.incrementAndGet();
            executor.submit(() -> {
                log.info("Downloading asset {}", url);
                downloadedAssets.add(url);
                try (HttpClient httpClient = HttpClient.newHttpClient()) {
                    HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(url)).build();
                    HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        if (config.get(CONFIG_ASSET_DATABASE).equals("true")) {
                            jooq.update(Tables.ASSET)
                                    .set(Tables.ASSET.DATA, response.body())
                                    .where(Tables.ASSET.ID.eq(id))
                                    .execute();
                        }

                        if (config.get(CONFIG_ASSET_FILESYSTEM).equals("true")) {
                            String name = jooq.select().from(Tables.ASSET)
                                    .where(Tables.ASSET.ID.eq(id))
                                    .fetch()
                                    .getValue(0, Tables.ASSET.NAME);
                            String contentType = response.headers().firstValue("Content-Type").orElse("application/octet-stream");
                            String extension = contentType.split("/")[1].replaceAll("\\+.*$", "");
                            String nameHash = DigestUtils.md5DigestAsHex(name.getBytes(StandardCharsets.UTF_8));
                            Files.write(
                                    Path.of(config.get(CONFIG_ASSET_FILESYSTEM_DIR), nameHash + "." + extension),
                                    response.body()
                            );
                        }
                    }
                } catch (Exception e) {
                    log.error("Error downloading asset {}", url, e);
                } finally {
                    log.info("Downloaded asset {}", url);
                    jobCounter.decrementAndGet();
                }
            });
        }
    }
}
