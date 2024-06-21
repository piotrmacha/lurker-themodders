package pl.piotrmacha.lurker.domain;

import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

@RequiredArgsConstructor
public abstract class AbstractPageProcessor implements PageProcessor {
    protected final DownloadService service;
    protected PageInfo page;
    protected Document document;

    public void prepare(PageInfo page, Document document) {
        this.page = page;
        this.document = document;
    }

    @Override
    public abstract void process(PageInfo page, Document document, PageProcessor context);

    @Override
    public DownloadService service() {
        return service;
    }

    @Override
    public Stream<Element> select(Element element, String selector) {
        return element.select(selector).stream();
    }

    @Override
    public Stream<Element> select(String selector) {
        return select(document, selector);
    }

    @Override
    public Optional<Element> selectFirst(Element element, String selector) {
        return Optional.ofNullable(element.selectFirst(selector));
    }

    @Override
    public Optional<Element> selectFirst(String selector) {
        return selectFirst(document, selector);
    }

    @Override
    public void forEach(Element element, String selector, Consumer<Element> processor) {
        select(element, selector).forEach(processor);
    }

    @Override
    public void forEach(String selector, Consumer<Element> processor) {
        forEach(document, selector, processor);
    }
}
