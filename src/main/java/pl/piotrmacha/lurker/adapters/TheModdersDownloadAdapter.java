package pl.piotrmacha.lurker.adapters;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import pl.piotrmacha.lurker.domain.DownloadAdapter;
import pl.piotrmacha.lurker.domain.DownloadContext;

import java.io.IOException;
import java.net.ConnectException;
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static pl.piotrmacha.lurker.domain.DownloadContext.CONFIG_HTTP_MAX_CONNECTIONS;

@Component("themodders")
public class TheModdersDownloadAdapter implements DownloadAdapter {
    private final static Logger log = org.slf4j.LoggerFactory.getLogger(TheModdersDownloadAdapter.class);
    private final static Pattern BOARD_PATTERN = Pattern.compile("board=([0-9]+)\\.([0-9]+)");
    private final static Pattern TOPIC_PATTERN = Pattern.compile("topic=([0-9]+)\\.([0-9]+)");
    private final static Pattern RECENT_PAGE_PATTERN = Pattern.compile("action=recent;start=([0-9]+)");

    private final AtomicInteger httpConnections = new AtomicInteger();
    private final Cache<String, Boolean> visitedUrlCache = CacheBuilder.newBuilder().maximumSize(10_000).weakKeys().weakValues().build();
    private final Cache<String, Boolean> savedCategoryCache = CacheBuilder.newBuilder().maximumSize(10_000).weakKeys().weakValues().build();
    private final Set<String> visitedUrlsRecent = new HashSet<>();

    @Override
    public void download(String url, String type, String id, DownloadContext context, Map<String, String> config) throws IOException {
        if (visitedUrlCache.getIfPresent(url) != null) {
            return;
        }

        visitedUrlCache.put(url, true);

        switch (type) {
            case "index" -> downloadIndex(url, context, config);
            case "category" -> downloadCategory(url, id, context, config);
            case "thread" -> downloadThread(url, id, context, config);
            case "recent" -> downloadRecent(url, context, config);
        }
    }

    private void downloadRecent(String url, DownloadContext context, Map<String, String> config) throws IOException {
        log.info("Downloading recent from {}", url);
        Document document = getDocument(url, config);

        document.select("#recent div.core_posts").forEach(root -> {
            String categoryId = null;
            String threadId = null;

            for (Element link : root.select(".topic_info a")) {
                String href = normalizeUrl(link.attr("href"));

                Matcher boardMatcher = BOARD_PATTERN.matcher(href);
                if (boardMatcher.find()) {
                    categoryId = boardMatcher.group(1);
                    String boardName = link.ownText();

                    if (!context.hasCategory(categoryId)) {
                        context.saveCategory(categoryId, boardName, href, "", null);
                    }

                    context.enqueueDownload(href, "category", categoryId, true);
                }

                Matcher topicMatcher = TOPIC_PATTERN.matcher(href);
                if (topicMatcher.find()) {
                    threadId = topicMatcher.group(1);
                    String topicName = link.ownText();

                    if (!context.hasThread(threadId)) {
                        context.saveThread(threadId, topicName, href, null, categoryId, null);
                    }

                    context.enqueueDownload(href, "thread", threadId, true);
                }
            }
        });

        document.select("a.navPages").forEach(a -> {
            Matcher recentPageMatcher = RECENT_PAGE_PATTERN.matcher(a.attr("href"));
            if (recentPageMatcher.find()) {
                String href = normalizeUrl(a.attr("href"));
                if (visitedUrlsRecent.contains(href)) {
                    return;
                }
                visitedUrlsRecent.add(href);
                context.enqueueDownload(href, "recent", recentPageMatcher.group(1));
            }
        });

        log.info("Recent downloaded from {}", url);
    }

    private void downloadIndex(String url, DownloadContext context, Map<String, String> config) throws IOException {
        log.info("Downloading index from {}", url);
        Document document = getDocument(url, config);
        document.select("a.info_text").forEach(a -> {
            processCategoryUrl(null, context, a, config);
        });
        log.info("Index downloaded from {}", url);
    }

    private void downloadCategory(String url, String id, DownloadContext context, Map<String, String> config) throws IOException {
        log.info("Downloading category from {}", url);
        Document document = getDocument(url, config);

        document.select("a.top_info").forEach(a -> {
            processCategoryUrl(id, context, a, config);
        });

        document.select("a.navPages").forEach(a -> {
            processCategoryUrl(id, context, a, config);
        });

        document.select("#messageindex a.all_td_link").forEach(a -> {
            processThreadUrl(id, context, a, config);
        });

        log.info("Category downloaded from {}", url);
    }

    private void downloadThread(String url, String id, DownloadContext context, Map<String, String> config) throws IOException {
        log.info("Downloading thread from {}", url);
        Document document = getDocument(url, config);

        Matcher matcher = TOPIC_PATTERN.matcher(url);
        if (matcher.find() && matcher.group(2).equalsIgnoreCase("0")) {
            Element firstPost = document.selectFirst("#forumposts .windowbg:first-of-type .post_wrapper");
            Element titleElement = document.selectFirst(".title_text");
            String title = titleElement != null ? titleElement.ownText() : "<???>";
            if (firstPost != null) {
                String authorId = null;
                String nickname = null;
                String avatar = null;
                Instant lastEditDate = null;

                Element nick = firstPost.selectFirst(".nick a");
                if (nick != null) {
                    Pattern accountPattern = Pattern.compile("u=([0-9]+)");
                    Matcher accountMatcher = accountPattern.matcher(nick.attr("href"));
                    nickname = nick.ownText();
                    if (accountMatcher.find()) {
                        authorId = accountMatcher.group(1);
                    }
                }

                Element avatarElement = firstPost.selectFirst("img.avatar");
                if (avatarElement != null) {
                    avatar = avatarElement.attr("src");
                }

                if (authorId != null) {
                    Long avatarId = null;
                    if (avatar != null) {
                        avatarId = context.saveAsset("avatar:" + authorId, avatar);
                    }
                    context.saveAccount(authorId, nickname, normalizeUrl("https://themodders.org/index.php?action=profile;u=" + authorId, true), avatarId);
                }

                Element lastEditDateElement = firstPost.selectFirst(".keyinfo");
                if (lastEditDateElement != null) {
                    Pattern datePatter = Pattern.compile("([0-9]{4}-[0-9]{2}-[0-9]{2}, [0-9]{2}:[0-9]{2})");
                    Matcher dateMatcher = datePatter.matcher(lastEditDateElement.text());
                    if (dateMatcher.find()) {
                        lastEditDate = Instant.parse(dateMatcher.group(1).replace(", ", "T") + ":00Z");
                    }
                }

                context.updateThread(id, title, authorId, lastEditDate);
            }
        }

        document.select("div.post_wrapper").forEach(post -> {
            processPost(id, normalizeUrl(url), context, post, config);
        });

        document.select("a.navPages").forEach(a -> {
            Matcher topicMatcher = TOPIC_PATTERN.matcher(a.attr("href"));
            if (topicMatcher.find()) {
                context.enqueueDownload(normalizeUrl(a.attr("href")), "thread", topicMatcher.group(1), topicMatcher.group(2).equals("0"));
            }
        });

        log.info("Thread downloaded from {}", url);
    }

    private void processPost(String threadId, String threadUrl, DownloadContext context, Element post, Map<String, String> config) {
        String postId = "";
        String content = "";
        String authorId = null;
        String nickname = null;
        String authorUrl = null;
        String avatar = null;
        Instant lastEditDate = null;

        Element nick = post.selectFirst(".nick a");
        if (nick != null) {
            Pattern accountPattern = Pattern.compile("u=([0-9]+)");
            Matcher accountMatcher = accountPattern.matcher(nick.attr("href"));
            nickname = nick.ownText();
            authorUrl = nick.attr("herf");
            if (accountMatcher.find()) {
                authorId = accountMatcher.group(1);
            }
        }

        Element avatarElement = post.selectFirst("img.avatar");
        if (avatarElement != null) {
            avatar = avatarElement.attr("src");
        }

        if (authorId != null) {
            Long avatarId = null;
            if (avatar != null) {
                avatarId = context.saveAsset("avatar:" + authorId, normalizeUrl(avatar));
            }
            context.saveAccount(authorId, nickname, authorUrl, avatarId);
        }

        Element lastEditDateElement = post.selectFirst(".keyinfo");
        if (lastEditDateElement != null) {
            Pattern datePatter = Pattern.compile("([0-9]{4}-[0-9]{2}-[0-9]{2}, [0-9]{2}:[0-9]{2})");
            Matcher dateMatcher = datePatter.matcher(lastEditDateElement.text());
            if (dateMatcher.find()) {
                lastEditDate = Instant.parse(dateMatcher.group(1).replace(", ", "T") + ":00Z");
            }
        }

        Element postInner = post.selectFirst(".post .inner");
        if (postInner != null) {
            postId = postInner.attr("id").replace("msg_", "");
            content = postInner.html();
            for (Element img : postInner.select("img")) {
                String src = img.attr("src");
                if (src.startsWith("http")) {
                    context.saveAsset("post:" + postId + ":image:" + src, normalizeUrl(src));
                }
            }
        }

        context.savePost(postId, content, threadUrl + ".msg" + postId + "#msg" + postId, authorId, threadId, lastEditDate);
    }

    private void processThreadUrl(String categoryId, DownloadContext context, Element a, Map<String, String> config) {
        String href = a.attr("href");
        Matcher matcher = TheModdersDownloadAdapter.TOPIC_PATTERN.matcher(href);
        if (matcher.find()) {
            String topicId = matcher.group(1);
            Element subjectElement = a.selectFirst(".message_link");
            String subject = subjectElement == null ? "" : subjectElement.ownText();

            if (matcher.group(2).equalsIgnoreCase("0")) {
                context.saveThread(topicId, subject, normalizeUrl(href, true), null, categoryId, null);
                context.enqueueDownload(normalizeUrl(href), "thread", topicId);
            }
        }
    }

    private void processCategoryUrl(String id, DownloadContext context, Element a, Map<String, String> config) {
        String href = a.attr("href");
        Matcher matcher = TheModdersDownloadAdapter.BOARD_PATTERN.matcher(href);
        if (matcher.find()) {
            String boardId = matcher.group(1);
            Element subjectElement = a.selectFirst(".subject");
            String subject = subjectElement == null ? "" : subjectElement.ownText();
            Element descriptionElement = a.selectFirst(".about_info");
            String description = descriptionElement == null ? "" : descriptionElement.ownText();

            if (savedCategoryCache.getIfPresent(boardId) == null && matcher.group(2).equalsIgnoreCase("0")) {
                context.saveCategory(boardId, subject, normalizeUrl(href, true), description, id);
                context.enqueueDownload(normalizeUrl(href), "category", boardId, true);
                savedCategoryCache.put(boardId, true);
            }
        }
    }

    private Document getDocument(String url, Map<String, String> config) throws IOException {
        return getDocument(url, 0, config);
    }

    private Document getDocument(String url, int retries, Map<String, String> config) throws IOException {
        if (retries > 10) {
            httpConnections.decrementAndGet();
            throw new RemoteException("Could not download document from " + url);
        }

        int maxConnections = Integer.parseInt(config.get(CONFIG_HTTP_MAX_CONNECTIONS));
        Instant start = Instant.now();
        while (httpConnections.getAndIncrement() > maxConnections) {
            httpConnections.decrementAndGet();
            if (Instant.now().minusSeconds(300).isAfter(start)) {
                throw new ConnectException(
                        "Couldn't download %s because of too many connections. Timeout after 300 seconds of waiting for a slot"
                                .formatted(url));
            }
            Thread.yield();
        }

        try {
            Random random = new Random();
            Thread.sleep((long) random.nextInt(100, 300) * retries);
        } catch (InterruptedException e) {
            httpConnections.decrementAndGet();
            throw new RuntimeException(e);
        }

        try {
            Document document = Jsoup.newSession().url(url).get();
            httpConnections.decrementAndGet();
            return document;
        } catch (Exception e) {
            log.warn("Could not download document from " + url, e);
            httpConnections.decrementAndGet();
            return getDocument(url, retries + 1, config);
        }
    }

    private String normalizeUrl(String url) {
        return normalizeUrl(url, false);
    }

    private String normalizeUrl(String url, boolean keepHash) {
        url = url
                .replaceAll("\\?PHPSESSID=[a-z0-9]+&", "?")
                .replaceAll("&PHPSESSID=[a-z0-9]+", "");
        if (!keepHash) {
            url = url.replaceAll("#.*$", "");
        }
        return url;
    }
}
