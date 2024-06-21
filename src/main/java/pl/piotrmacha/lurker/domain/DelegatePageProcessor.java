package pl.piotrmacha.lurker.domain;

import org.jsoup.nodes.Document;

public class DelegatePageProcessor extends AbstractPageProcessor {
    private PageProcessorFn delegate;

    public DelegatePageProcessor(DownloadService service) {
        super(service);
    }

    public void setDelegate(PageProcessorFn delegate) {
        this.delegate = delegate;
    }

    @Override
    public void process(PageInfo page, Document document, PageProcessor context) {
        prepare(page, document);
        delegate.process(page, document, context);
    }
}
