package pl.piotrmacha.lurker.domain.processor;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import pl.piotrmacha.lurker.domain.*;

import java.net.URI;

@Slf4j
public class IndexPageProcessor extends DelegatePageProcessor {
    public IndexPageProcessor(DownloadService service) {
        super(service);
        setDelegate(this::processIndex);
    }

    private void processIndex(PageInfo pageInfo, Document document, PageProcessor context) {
        forEach(".board a.info_text", this::processBoardElement);
    }

    private void processBoardElement(Element element) {
        String href = element.attr("href");
        PageInfo.Uri.Board uri = new PageInfo.Uri.Board(URI.create(href)).normalize();
        String boardName = selectFirst(element, ".subject").map(Element::text).orElse("Unknown Board");
        String boardDescription = selectFirst(element, ".about_info").map(Element::text).orElse("");

        Board board = Board.of(String.valueOf(uri.id()), uri.normalize().toString(), boardName, boardDescription);
        Board.dao().save(board);

        service().addTask(DownloadQueue.TaskType.BOARD, uri.asPageUri(), (long) uri.id());
    }
}

