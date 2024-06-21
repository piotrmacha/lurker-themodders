package pl.piotrmacha.lurker.domain;

import com.github.rholder.retry.*;
import com.google.common.collect.Comparators;
import com.google.common.util.concurrent.RateLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import pl.piotrmacha.lurker.domain.processor.BoardPageProcessor;
import pl.piotrmacha.lurker.domain.processor.IndexPageProcessor;
import pl.piotrmacha.lurker.domain.processor.NewPostsPageProcessor;
import pl.piotrmacha.lurker.domain.processor.TopicPageProcessor;
import pl.piotrmacha.lurker.jooq.Tables;
import sun.misc.Signal;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@ShellComponent
@RequiredArgsConstructor
public class DownloadService {
    private final DownloadQueue queue;
    private final DSLContext jooq;
    private final AbstractPageProcessor indexPageProcessor = new IndexPageProcessor(this);
    private final AbstractPageProcessor boardPageProcessor = new BoardPageProcessor(this);
    private final AbstractPageProcessor topicPageProcessor = new TopicPageProcessor(this);
    private final AbstractPageProcessor newPostsPageProcessor = new NewPostsPageProcessor(this);
    private final ExecutorService executor = Executors.newFixedThreadPool(64, new ThreadFactoryBuilder()
            .setThreadFactory(Thread.ofPlatform().factory())
            .setNameFormat("executor-%d")
            .build());
    private final RateLimiter httpRateLimiter = RateLimiter.create(5.0);
    private Path assetsDirectory = Path.of("./assets/");

    @ShellMethod(value = "Build download queue", key = "build-index")
    public void buildIndex(
            @ShellOption(value = "--uri", defaultValue = "https://themodders.org") URI uri
    ) throws IOException {
        PageInfo index = new PageInfo.Uri(uri);
        Document document = getDocument(uri.toString());
        indexPageProcessor.process(index, document, indexPageProcessor);
        Signal.handle(new Signal("INT"), signal -> {
            log.info("Existing...");
            executor.shutdown();
            System.exit(0);
        });

        List<Future<TaskResult>> tasks = new ArrayList<>();
        int tasksEmptyTimes = 0;
        while (queue.size() > 0 || !tasks.isEmpty()) {
            queue.poll(Set.of(DownloadQueue.TaskType.BOARD))
                    .map(task -> (Callable<TaskResult>) () -> processTask(task))
                    .map(executor::submit)
                    .ifPresent(tasks::add);

            List<Future<TaskResult>> futuresDone = tasks.stream().filter(Future::isDone).peek(Future::resultNow).toList();
            tasks.removeAll(futuresDone);

            if (tasks.isEmpty()) {
                tasksEmptyTimes++;
                if (tasksEmptyTimes > 100) {
                    break;
                }
            } else {
                tasksEmptyTimes = 0;
            }
        }

        log.info("Index built successfully");
        executor.shutdown();
    }

    @ShellMethod(value = "Build download queue from new posts", key = "build-new-posts")
    public void buildNewPosts(
            @ShellOption(value = "--uri", defaultValue = "https://themodders.org/index.php?action=recent") URI uri
    ) throws IOException {
        Signal.handle(new Signal("INT"), signal -> {
            log.info("Existing...");
            executor.shutdown();
            System.exit(0);
        });

        for (int i = 0; i < 100; i += 10) {
            PageInfo index = new PageInfo.Uri(URI.create(uri.toString() + ";start=" + i));
            Document document = getDocument(index.uri().toString());
            newPostsPageProcessor.process(index, document, newPostsPageProcessor);
        }

        log.info("New posts index built");
        executor.shutdown();
    }

    @ShellMethod(value = "Download topics from queue", key = "download-topics")
    public void downloadTopics(
            @ShellOption(value = "--dir", defaultValue = "./assets/") String dir
    ) throws IOException {
        if (dir != null && !dir.isBlank()) {
            assetsDirectory = Path.of(dir);
        }
        Signal.handle(new Signal("INT"), signal -> {
            log.info("Existing...");
            executor.shutdown();
            System.exit(0);
        });

        jooq.update(Tables.DOWNLOAD_QUEUE)
                .set(Tables.DOWNLOAD_QUEUE.LOCKED_AT, (OffsetDateTime) null)
                .execute();

        List<Future<TaskResult>> tasks = new ArrayList<>();
        int tasksEmptyTimes = 0;
        while (queue.size(Set.of(DownloadQueue.TaskType.TOPIC, DownloadQueue.TaskType.ASSET)) > 0 || !tasks.isEmpty()) {
            queue.poll(Set.of(DownloadQueue.TaskType.TOPIC, DownloadQueue.TaskType.ASSET))
                    .map(task -> (Callable<TaskResult>) () -> processTask(task))
                    .map(executor::submit)
                    .ifPresent(tasks::add);

            List<Future<TaskResult>> futuresDone = tasks.stream().filter(Future::isDone).peek(Future::resultNow).toList();
            tasks.removeAll(futuresDone);

            if (tasks.isEmpty()) {
                tasksEmptyTimes++;
                if (tasksEmptyTimes > 100) {
                    break;
                }
            } else {
                tasksEmptyTimes = 0;
            }
        }

        log.info("Download finished");
        executor.shutdown();
    }

    @ShellMethod(value = "Clear download queue", key = "clear-download-queue")
    public void clearDownloadQueue() throws IOException {
        jooq.deleteFrom(Tables.DOWNLOAD_QUEUE).execute();
    }

    @ShellMethod(value = "Clear all", key = "clear-all")
    public void clearEverything() throws IOException {
        jooq.truncate(Tables.DOWNLOAD_QUEUE_SCHEDULED).cascade().execute();
        jooq.truncate(Tables.DOWNLOAD_QUEUE_DONE).cascade().execute();
        jooq.truncate(Tables.DOWNLOAD_QUEUE_FAILURE).cascade().execute();
        jooq.truncate(Tables.DOWNLOAD_QUEUE).cascade().execute();
        jooq.truncate(Tables.POST_FULLTEXT).cascade().execute();
        jooq.truncate(Tables.POST_ATTACHMENT).cascade().execute();
        jooq.truncate(Tables.POST).cascade().execute();
        jooq.truncate(Tables.TOPIC).cascade().execute();
        jooq.truncate(Tables.BOARD).cascade().execute();
        jooq.truncate(Tables.ACCOUNT).cascade().execute();
        jooq.truncate(Tables.ASSET).cascade().execute();
    }

    public void addTask(DownloadQueue.TaskType type, PageInfo.Uri pageInfo, Long entityId) {
        queue.enqueue(type, pageInfo, entityId);
    }

    private TaskResult processTask(DownloadQueue.Task task) {
        log.info("Processing task: {}", task);
        try {
            switch (task.type()) {
                case BOARD -> {
                    PageInfo pageInfo = new PageInfo.Uri(URI.create(task.url()));
                    Document document = getDocument(task.url());
                    boardPageProcessor.process(pageInfo, document, boardPageProcessor);
                }
                case TOPIC -> {
                    PageInfo pageInfo = new PageInfo.Uri(URI.create(task.url()));
                    Document document = getDocument(task.url());
                    topicPageProcessor.process(pageInfo, document, topicPageProcessor);
                }
                case ASSET -> {
                    try {
                        HttpResponse<byte[]> file = getFile(task.url());
                        Asset asset = Asset.dao().get(task.entityId());
                        String mimeType = file.headers().firstValue("Content-Type").orElse("application/octet-stream");
                        String extension = Arrays.stream(mimeType.split("/")).max(Comparator.naturalOrder()).orElse(".bin");
                        String filename = asset.path() + "." + extension;
                        Path path = Path.of(assetsDirectory.toString(), filename);
                        Files.createDirectories(path.getParent());
                        Files.write(path, file.body());
                        asset.withDownloadInfo(mimeType, filename, (long) file.body().length).save();
                    } catch (Exception e) {
                        log.error("Couldn't download asset from {}", task.url());
                    }
                }
                default -> throw new IllegalArgumentException("Unsupported task type: " + task.type());
            }
            DownloadQueue.TaskDone taskDone = task.done();
            log.info("Task done: {}", task);
            return new TaskResult.Done(taskDone);
        } catch (Exception e) {
            log.error("Error processing task: {}", task, e);
            return new TaskResult.Failure(task.failure(e.getClass().getName(), e.getMessage()));
        }
    }

    private Document getDocument(String url) throws IOException {
        Retryer<Document> retryer = RetryerBuilder.<Document>newBuilder()
                .retryIfExceptionOfType(IOException.class)
                .retryIfRuntimeException()
                .withWaitStrategy(WaitStrategies.randomWait(1, TimeUnit.SECONDS, 10, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterDelay(1, TimeUnit.MINUTES))
                .build();
        try {
            return retryer.call(() -> {
                httpRateLimiter.acquire();
                return Jsoup.connect(url).get();
            });
        } catch (ExecutionException | RetryException e) {
            log.error("Error fetching document: {}", url, e);
            throw new RuntimeException(e);
        }
    }

    private HttpResponse<byte[]> getFile(String url) throws IOException {
        Retryer<HttpResponse<byte[]>> retryer = RetryerBuilder.<HttpResponse<byte[]>>newBuilder()
                .retryIfExceptionOfType(IOException.class)
                .retryIfRuntimeException()
                .withWaitStrategy(WaitStrategies.randomWait(1, TimeUnit.SECONDS, 10, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterDelay(10, TimeUnit.SECONDS))
                .retryIfResult(r -> r.statusCode() < 200 || r.statusCode() > 399)
                .build();
        try {
            return retryer.call(() -> {
                try (HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()) {
                    HttpRequest request = HttpRequest.newBuilder(URI.create(url)).build();
                    return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                }
            });
        } catch (ExecutionException | RetryException e) {
            throw new RuntimeException(e);
        }
    }

    sealed interface TaskResult {
        record Done(DownloadQueue.TaskDone task) implements TaskResult {}

        record Failure(DownloadQueue.TaskFailure task) implements TaskResult {}
    }
}
