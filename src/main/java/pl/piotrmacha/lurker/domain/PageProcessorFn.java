package pl.piotrmacha.lurker.domain;

import org.jsoup.nodes.Document;

@FunctionalInterface
public interface PageProcessorFn {
    void process(PageInfo page, Document document, PageProcessor context);
}
