package pl.piotrmacha.lurker.domain;

import java.io.IOException;
import java.util.Map;

public interface DownloadAdapter {
    void download(String url, String type, String id, DownloadContext context, Map<String, String> config) throws IOException, IOException;
}
