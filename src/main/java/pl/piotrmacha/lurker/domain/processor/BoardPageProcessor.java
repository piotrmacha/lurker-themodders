package pl.piotrmacha.lurker.domain.processor;

import lombok.extern.slf4j.Slf4j;
import org.jooq.exception.IntegrityConstraintViolationException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import pl.piotrmacha.lurker.domain.*;

import java.net.URI;
import java.util.Comparator;

@Slf4j
public class BoardPageProcessor extends DelegatePageProcessor {
    public BoardPageProcessor(DownloadService service) {
        super(service);
        setDelegate(this::processBoard);
    }

    private void processBoard(PageInfo pageInfo, Document document, PageProcessor context) {
        PageInfo.Uri.Board pageUri = new PageInfo.Uri.Board(pageInfo.uri());
        if (pageUri.offset() == 0) {
            schedulePages(pageInfo, pageUri);
        }

        forEach(".board a.top_info", this::processBoardElement);
        forEach("#messageindex a.all_td_link", this::processTopicElement);
    }

    private void schedulePages(PageInfo pageInfo, PageInfo.Uri.Board pageUri) {
        int lastOffset = select("#main_content .navigate_section a.navPages")
                .map(e -> e.attr("href"))
                .map(href -> {
                    String[] split = href.split("\\.");
                    return Integer.parseInt(split[split.length - 1]);
                })
                .max(Comparator.naturalOrder())
                .orElse(0);

        int increment = 20;
        for (int i = increment; i <= lastOffset; i += increment) {
            PageInfo.Uri.Board uri = pageUri.withOffset(i).normalizeOffset();
            service().addTask(DownloadQueue.TaskType.BOARD, uri.asPageUri(), (long) uri.id());
        }
    }

    private void processBoardElement(Element element) {
        String href = element.attr("href");
        PageInfo.Uri.Board uri = new PageInfo.Uri.Board(URI.create(href)).normalizeOffset();
        String boardName = selectFirst(element, ".subject").map(Element::text).orElse("Unknown Board");
        String boardDescription = selectFirst(element, ".about_info").map(Element::text).orElse("");

        long parentOid = new PageInfo.Uri(page.uri()).asBoard().id();
        Board parent = Board.dao().findByOid(String.valueOf(parentOid))
                .orElseThrow(() -> new RuntimeException("Parent board not found oid=" + parentOid));

        if (Board.dao().findByOid(String.valueOf(uri.id())).isPresent()) {
            service().addTask(DownloadQueue.TaskType.BOARD, uri.asPageUri(), (long) uri.id());
            return;
        }

        Board board = Board.of(String.valueOf(uri.id()), uri.normalize().toString(), boardName, boardDescription, parent.id());
        try {
            Board.dao().save(board);
        } catch (IntegrityConstraintViolationException e) {
            // ignore, board exists
        }

        service().addTask(DownloadQueue.TaskType.BOARD, uri.asPageUri(), (long) uri.id());
    }

    private void processTopicElement(Element element) {
        String href = element.attr("href");
        PageInfo.Uri.Topic uri = new PageInfo.Uri.Topic(URI.create(href)).normalize();
        String title = selectFirst(element, ".message_link").map(Element::text).orElse("Unknown Topic");

        long boardOid = new PageInfo.Uri(page.uri()).asBoard().id();
        Board board = Board.dao().findByOid(String.valueOf(boardOid))
                .orElseThrow(() -> new RuntimeException("Board not found oid=" + boardOid));

        if (Topic.dao().findByOid(String.valueOf(uri.id())).isPresent()) {
            service().addTask(DownloadQueue.TaskType.TOPIC, uri.asPageUri(), (long) uri.id());
            return;
        }

        Topic topic = Topic.of(String.valueOf(uri.id()), uri.normalize().toString(), board.id()).withTitle(title);
        try {
            Topic.dao().save(topic);
        } catch (IntegrityConstraintViolationException e) {
            // ignore, topic exists
        }

        service().addTask(DownloadQueue.TaskType.TOPIC, uri.asPageUri(), (long) uri.id());
    }
}
