package pl.piotrmacha.lurker.domain.processor;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import pl.piotrmacha.lurker.domain.*;
import pl.piotrmacha.lurker.jooq.Tables;

import java.net.URI;
import java.util.List;

@Slf4j
public class NewPostsPageProcessor extends DelegatePageProcessor {
    public NewPostsPageProcessor(DownloadService service) {
        super(service);
        setDelegate(this::processNewPosts);
    }

    private void processNewPosts(PageInfo pageInfo, Document document, PageProcessor context) {
        log.info("Processing new posts page {}", pageInfo.uri());
        forEach(document, "#recent .core_posts", this::processTopic);
        log.info("Finished page {}", pageInfo.uri());
    }

    private void processTopic(Element root) {
        String boardOid = selectFirst(root, ".topic_info h5 a:nth-of-type(1)").map(e -> e.attr("href"))
                .map(url -> {
                    String[] split = url.split("=");
                    return split[split.length - 1].split("\\.")[0];
                })
                .orElse("none");
        String url = selectFirst(root, ".topic_info h5 a:nth-of-type(2)").map(e -> e.attr("href")).orElse(null);
        Board board = Board.dao().findByOid(boardOid).orElse(null);
        if (url != null && board != null) {
            PageInfo.Uri.Topic uri = new PageInfo.Uri.Topic(URI.create(url));
            String title =  selectFirst(root, ".topic_info h5 a:nth-of-type(2)").map(Element::text).orElse("Unknown Topic");
            String topicOid = String.valueOf(uri.id());
            Topic topic = Topic.dao().findByOid(topicOid)
                    .orElseGet(() -> Topic.of(String.valueOf(uri.id()), uri.normalize().toString(), board.id()).withTitle(title));
            topic.withTitle(title).save();
            service().addTask(DownloadQueue.TaskType.TOPIC, uri.asPageUri(), (long) uri.id());
        }
    }
}
