package pl.piotrmacha.lurker.command;

import org.springframework.context.ApplicationContext;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import pl.piotrmacha.lurker.domain.DownloadAdapter;
import pl.piotrmacha.lurker.domain.DownloadContext;
import pl.piotrmacha.lurker.domain.DownloadService;

import java.util.Map;

import static pl.piotrmacha.lurker.domain.DownloadContext.*;

@ShellComponent
public class DownloadCommand {
    private final ApplicationContext context;
    private final DownloadService downloadService;

    public DownloadCommand(ApplicationContext context, DownloadService downloadService) {
        this.context = context;
        this.downloadService = downloadService;
    }

    @ShellMethod(key = "download", value = "Downlaod whole forum")
    public void download(
            @ShellOption(defaultValue = "https://themodders.org") String url,
            @ShellOption(defaultValue = "themodders") String adapter,
            @ShellOption(value = "--save-visited", defaultValue = "true", help = "Save visited URLs in database") Boolean saveVisited,
            @ShellOption(value = "--clear-visited", defaultValue = "false", help = "Clear visited URLs at the start") Boolean clearVisited,
            @ShellOption(value = "--skip-visited", defaultValue = "false", help = "Skip already visited URLs") Boolean skipVisited,
            @ShellOption(value = "--asset-database", defaultValue = "true", help = "Save assets in database") Boolean assetDatabase,
            @ShellOption(value = "--asset-filesystem", defaultValue = "false", help = "Save assets as files") Boolean assetFilesystem,
            @ShellOption(value = "--asset-dir", defaultValue = "./assets", help = "Assets directory") String assetDir,
            @ShellOption(value = "--threads", defaultValue = "64", help = "How many threads to use") Integer threads,
            @ShellOption(value = "--threads-virtual", defaultValue = "false", help = "Use virtual threads") Boolean threadsVirtual,
            @ShellOption(value = "--http-connections", defaultValue = "32", help = "Max HTTP connections at the same time") Integer maxConnections
    ) {
        DownloadAdapter adapterClass = context.getBean(adapter, DownloadAdapter.class);
        downloadService.download(adapterClass, url, "index", Map.of(
                CONFIG_SAVE_VISITED, saveVisited.toString(),
                CONFIG_CLEAR_VISITED, clearVisited.toString(),
                CONFIG_SKIP_VISITED, skipVisited.toString(),
                CONFIG_ASSET_DATABASE, assetDatabase.toString(),
                CONFIG_ASSET_FILESYSTEM, assetFilesystem.toString(),
                CONFIG_ASSET_FILESYSTEM_DIR, assetDir,
                CONFIG_THREAD_POOL_THREADS, threads.toString(),
                CONFIG_THREAD_POOL_VIRTUAL, threadsVirtual.toString(),
                CONFIG_HTTP_MAX_CONNECTIONS, maxConnections.toString()
        ));
    }

    @ShellMethod(key = "download-recent", value = "Download recent threads")
    public void downloadRecent(
            @ShellOption(defaultValue = "https://themodders.org/index.php?action=recent") String url,
            @ShellOption(defaultValue = "themodders") String adapter,
            @ShellOption(value = "--save-visited", defaultValue = "true", help = "Save visited URLs in database") Boolean saveVisited,
            @ShellOption(value = "--clear-visited", defaultValue = "false", help = "Clear visited URLs at the start") Boolean clearVisited,
            @ShellOption(value = "--skip-visited", defaultValue = "false", help = "Skip already visited URLs") Boolean skipVisited,
            @ShellOption(value = "--asset-database", defaultValue = "true", help = "Save assets in database") Boolean assetDatabase,
            @ShellOption(value = "--asset-filesystem", defaultValue = "false", help = "Save assets as files") Boolean assetFilesystem,
            @ShellOption(value = "--asset-dir", defaultValue = "./assets", help = "Assets directory") String assetDir,
            @ShellOption(value = "--threads", defaultValue = "64", help = "Use virtual threads") Integer threads,
            @ShellOption(value = "--threads-virtual", defaultValue = "false", help = "How many threads to use") Boolean threadsVirtual,
            @ShellOption(value = "--http-connections", defaultValue = "32", help = "Max HTTP connections at the same time") Integer maxConnections
    ) {
        DownloadAdapter adapterClass = context.getBean(adapter, DownloadAdapter.class);
        downloadService.download(adapterClass, url, "recent", Map.of(
                CONFIG_SAVE_VISITED, saveVisited.toString(),
                CONFIG_CLEAR_VISITED, clearVisited.toString(),
                CONFIG_SKIP_VISITED, skipVisited.toString(),
                CONFIG_ASSET_DATABASE, assetDatabase.toString(),
                CONFIG_ASSET_FILESYSTEM, assetFilesystem.toString(),
                CONFIG_ASSET_FILESYSTEM_DIR, assetDir,
                CONFIG_THREAD_POOL_THREADS, threads.toString(),
                CONFIG_THREAD_POOL_VIRTUAL, threadsVirtual.toString(),
                CONFIG_HTTP_MAX_CONNECTIONS, maxConnections.toString()
        ));
    }
}
