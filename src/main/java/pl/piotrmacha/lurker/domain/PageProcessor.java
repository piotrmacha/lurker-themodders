package pl.piotrmacha.lurker.domain;

import org.jsoup.nodes.Element;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public interface PageProcessor extends PageProcessorFn {
    DownloadService service();

    Stream<Element> select(Element root, String selector);

    Stream<Element> select(String selector);

    Optional<Element> selectFirst(Element root, String selector);

    Optional<Element> selectFirst(String selector);

    void forEach(Element element, String selector, Consumer<Element> processor);

    void forEach(String selector, Consumer<Element> processor);

}
