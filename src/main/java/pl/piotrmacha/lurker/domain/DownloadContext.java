package pl.piotrmacha.lurker.domain;

import java.time.Instant;

public interface DownloadContext {
    String CONFIG_SAVE_VISITED = "visited.save";
    String CONFIG_CLEAR_VISITED = "visited.clear";
    String CONFIG_SKIP_VISITED = "visited.skip";
    String CONFIG_ASSET_DATABASE = "asset.database";
    String CONFIG_ASSET_FILESYSTEM = "asset.filesystem";
    String CONFIG_ASSET_FILESYSTEM_DIR = "asset.filesystem.dir";
    String CONFIG_THREAD_POOL_THREADS = "thread_pool.threads";
    String CONFIG_THREAD_POOL_VIRTUAL = "thread_pool.virtual";
    String CONFIG_HTTP_MAX_CONNECTIONS = "http.max_connections";

    Long saveAsset(String name, String url);

    String saveAccount(String id, String name, String url, Long avatarId);

    String saveCategory(String id, String name, String url, String description, String parentId);

    String saveThread(String id, String title, String url, String authorId, String categoryId, Instant createdAt);

    String updateThread(String id, String title, String authorId, Instant createdAt);

    String savePost(String id, String content, String url, String authorId, String threadId, Instant createdAt);

    boolean hasCategory(String id);

    boolean hasThread(String id);

    void enqueueDownload(String url, String type, String id);

    void enqueueDownload(String url, String type, String id, boolean force);
}
