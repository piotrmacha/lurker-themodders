package pl.piotrmacha.lurker.domain.processor;

import org.jooq.exception.IntegrityConstraintViolationException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import pl.piotrmacha.lurker.domain.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TopicPageProcessor extends DelegatePageProcessor {
    public TopicPageProcessor(DownloadService service) {
        super(service);
        setDelegate(this::processTopic);
    }

    private void processTopic(PageInfo pageInfo, Document document, PageProcessor context) {
        PageInfo.Uri.Topic topicUri = new PageInfo.Uri.Topic(pageInfo.uri());
        Topic topic = Topic.dao().getByOid(String.valueOf(topicUri.id()));
        int offset = topicUri.offset();

        if (offset == 0) {
            processTopicAuthor(topic);
            processPosts(topic);
            schedulePages(topicUri);
        } else {
            processPosts(topic);
        }
    }

    private void processTopicAuthor(Topic topic) {
        Account account = selectFirst("#forumposts .windowbg:first-of-type")
                .map(this::processAccount)
                .orElseThrow(() -> new RuntimeException("Invalid structure"));

        topic.withAuthorId(account.id()).save();
    }

    private void processPosts(Topic topic) {
        select("#forumposts .windowbg").forEach(e -> processPost(e, topic));
    }

    private void processPost(Element root, Topic topic) {
        Account account = processAccount(root);
        Instant createdAt = Instant.now();

        Matcher dateMatcher = Pattern.compile("([0-9]{4}-[0-9]{2}-[0-9]{2}), ([0-9]{2}:[0-9]{2})").matcher(root.text());
        if (dateMatcher.find()) {
            String date = dateMatcher.group(1);
            String time = dateMatcher.group(2);
            createdAt = Instant.parse(date + "T" + time + ":00Z");
        }

        Element body = selectFirst(root, ".post .inner").orElseThrow(() -> new RuntimeException("Invalid structure"));
        String postOid = body.id().split("_")[1];
        String content = body.html();
        String url = topic.url() + ";msg" + postOid;

        final Instant createdAtFinal = createdAt;
        Post post = Post.dao().findByOid(postOid)
                .map(p -> p.withContent(content).withLastUpdate(Instant.now()).save())
                .orElseGet(() -> {
                    try {
                        return Post.of(postOid, url, account.id(), topic.id(), content, createdAtFinal)
                                .withContent(content)
                                .save();
                    } catch (IntegrityConstraintViolationException e) {
                        return Post.dao().findByOid(postOid)
                                .map(p -> p.withContent(content).withLastUpdate(Instant.now()).save())
                                .orElseThrow(() -> new RuntimeException("Post not found"));
                    }
                });

        AtomicInteger attachmentCounter = new AtomicInteger(0);
        select(body, "img").forEach(img -> {
            String src = img.attr("src");
            Asset asset = createAsset("topic/" + topic.oid() + "/post-" + postOid + "/" + attachmentCounter.getAndIncrement(), src);
            Post.dao().linkAsset(post, asset.id());
        });
    }

    private void schedulePages(PageInfo.Uri.Topic topicUri) {
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
            PageInfo.Uri.Topic uri = topicUri.normalizeOffset().withOffset(i);
            service().addTask(DownloadQueue.TaskType.TOPIC, uri.asPageUri(), (long) uri.id());
        }
    }

    private Account processAccount(Element postRoot) {
        return selectFirst(".poster")
                .map(element -> {
                    Element nick = selectFirst(element, ".nick a")
                            .or(() -> selectFirst(".nick"))
                            .orElseThrow(() -> new RuntimeException("Invalid structure"));
                    String username = nick.text();
                    String accountUrl = nick.hasAttr("href") ? nick.attr("href") : "guest_" + username;
                    String accountOid = Arrays.stream(accountUrl.split("=")).max(Comparator.naturalOrder()).orElse("guest_" + username);

                    return Account.dao().findByOid(accountOid)
                            .map(account -> {
                                Element avatar = selectFirst(element, "img.avatar").orElse(null);
                                if (avatar != null) {
                                    String avatarUrl = avatar.attr("src");
                                    Asset asset = createAsset("avatar/" + accountOid, avatarUrl);
                                    account.withAvatarId(asset.id()).save();
                                }
                                return account;
                            })
                            .orElseGet(() -> {
                                Element avatar = selectFirst(element, "img.avatar").orElse(null);
                                if (avatar != null) {
                                    String avatarUrl = avatar.attr("src");
                                    Asset asset = createAsset("avatar/" + accountOid, avatarUrl);
                                    try {
                                        return Account.of(accountOid, accountUrl, username, asset.id()).save();
                                    } catch (IntegrityConstraintViolationException e) {
                                        return Account.dao().findByOid(accountOid)
                                                .map(account -> account.withAvatarId(asset.id()).save())
                                                .orElseThrow(() -> new RuntimeException("Account not found"));
                                    }
                                }

                                try {
                                    return Account.of(accountOid, accountUrl, username, null).save();
                                } catch (IntegrityConstraintViolationException e) {
                                    return Account.dao().findByOid(accountOid)
                                            .map(account -> account.withAvatarId(null).save())
                                            .orElseThrow(() -> new RuntimeException("Account not found"));
                                }
                            });
                })
                .orElseThrow(() -> new RuntimeException("Invalid structure"));
    }

    private Asset createAsset(String name, String url) {
        return Asset.dao().findByUrl(url)
                .orElseGet(() -> {
                    try {
                        Asset asset = Asset.of(name, url).withPath(name).save();
                        DownloadQueue.Task.of(DownloadQueue.TaskType.ASSET, asset.url(), asset.id()).save();
                        return asset;
                    } catch (IntegrityConstraintViolationException e) {
                        return Asset.dao().findByUrl(url)
                                .orElseThrow(() -> new RuntimeException("Asset not found"));
                    }
                });
    }
}
