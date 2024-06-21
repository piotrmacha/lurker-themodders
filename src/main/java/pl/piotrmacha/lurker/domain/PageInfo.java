package pl.piotrmacha.lurker.domain;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public sealed interface PageInfo {
    URI uri();

    record Uri(URI uri) implements PageInfo {
        public Uri normalize() {
            String url = uri.toString();
            url = url.replaceAll("#.*$", "");
            url = url.replaceAll("\\?PHPSESSID=[^&]+(&)?", "?");
            url = url.replaceAll("&PHPSESSID=[^&]+", "");
            return new Uri(URI.create(url).normalize());
        }

        public Board asBoard() {
            return new Board(uri);
        }

        public Topic asTopic() {
            return new Topic(uri);
        }

        public Post asPost() {
            return new Post(uri);
        }

        @Override
        public String toString() {
            return uri.toString();
        }

        public record Board(URI uri, int id, int offset) implements PageInfo {
            static final Pattern pattern = Pattern.compile("board=([0-9]+)(\\.[0-9]+)?");

            public Board(URI uri) {
                this(uri, getId(uri), getOffset(uri));
            }

            public Uri asPageUri() {
                return new Uri(URI.create(uri.toString()));
            }

            public Board normalize() {
                String url = uri.toString();
                url = url.replaceAll("#.*$", "");
                url = url.replaceAll("\\?PHPSESSID=[^&]+(&)?", "?");
                url = url.replaceAll("&PHPSESSID=[^&]+", "");
                url = url.replaceAll("board=([0-9]+)\\.?([0-9]+)?", "board=$1");
                return new Board(URI.create(url).normalize(), id, 0);
            }

            public Board normalizeOffset() {
                String url = uri.toString();
                url = url.replaceAll("#.*$", "");
                url = url.replaceAll("\\?PHPSESSID=[^&]+(&)?", "?");
                url = url.replaceAll("&PHPSESSID=[^&]+", "");
                url = url.replaceAll("board=([0-9]+)(\\.?[0-9]+)?", "board=$1$2");
                return new Board(URI.create(url).normalize(), id, offset);
            }

            static int getId(URI uri) {
                Matcher matcher = pattern.matcher(uri.toString());
                if (matcher.find()) {
                    return Integer.parseInt(matcher.group(1));
                } else {
                    throw new IllegalArgumentException("Invalid board URI: " + uri);
                }
            }

            static int getOffset(URI uri) {
                Matcher matcher = pattern.matcher(uri.toString());
                if (matcher.find()) {
                    String page = matcher.group(2);
                    return page != null ? Integer.parseInt(page.replace(".", "")) : 0;
                } else {
                    throw new IllegalArgumentException("Invalid board URI: " + uri);
                }
            }

            static boolean isBoard(URI uri) {
                return pattern.matcher(uri.toString()).find();
            }

            @Override
            public String toString() {
                return uri.toString();
            }

            public Board withOffset(int newOffset) {
                if (newOffset < 0) {
                    throw new IllegalArgumentException("Offset must be greater than 0");
                }
                String newUrl = uri.toString().replaceAll("board=([0-9]+)(\\.?[0-9]+)?", "board=$1." + newOffset);
                return new Board(URI.create(newUrl), id, newOffset);
            }
        }

        public record Topic(URI uri, int id, int offset) implements PageInfo {
            static final Pattern pattern = Pattern.compile("topic=([0-9]+)\\.?([0-9]+)?(\\.msg[0-9]+)?(;topicseen)?");

            public Topic(URI uri) {
                this(uri, getId(uri), getOffset(uri));
            }

            public Uri asPageUri() {
                return new Uri(uri);
            }

            public Topic normalize() {
                String url = uri.toString();
                url = url.replaceAll("#.*$", "");
                url = url.replaceAll("\\?PHPSESSID=[^&]+(&)?", "?");
                url = url.replaceAll("&PHPSESSID=[^&]+", "");
                url = url.replaceAll("\\?topic=([0-9]+)\\.?([0-9]+)?(\\.msg[0-9]+)?(;topicseen)?", "?topic=$1");
                return new Topic(URI.create(url).normalize(), id, 0);
            }

            public Topic normalizeOffset() {
                String url = uri.toString();
                url = url.replaceAll("#.*$", "");
                url = url.replaceAll("\\?PHPSESSID=[^&]+(&)?", "?");
                url = url.replaceAll("&PHPSESSID=[^&]+", "");
                url = url.replaceAll("\\?topic=([0-9]+)(\\.?[0-9]+)?(\\.msg[0-9]+)?(;topicseen)?", "?topic=$1$2");
                return new Topic(URI.create(url).normalize(), id, offset);
            }

            static int getId(URI uri) {
                Matcher matcher = pattern.matcher(uri.toString());
                if (matcher.find()) {
                    return Integer.parseInt(matcher.group(1));
                } else {
                    throw new IllegalArgumentException("Invalid topic URI: " + uri);
                }
            }

            static int getOffset(URI uri) {
                Matcher matcher = pattern.matcher(uri.toString());
                if (matcher.find()) {
                    String page = matcher.group(2);
                    return page != null ? Integer.parseInt(page) : 0;
                } else {
                    throw new IllegalArgumentException("Invalid topic URI: " + uri);
                }
            }

            static boolean isTopic(URI uri) {
                return pattern.matcher(uri.toString()).find();
            }

            @Override
            public String toString() {
                return uri.toString();
            }

            public Topic withOffset(int newOffset) {
                if (newOffset < 0) {
                    throw new IllegalArgumentException("Offset must be greater than 0");
                }
                String newUrl = uri.toString().replaceAll(
                        "topic=([0-9]+)\\.?([0-9]+)?(\\.msg[0-9]+)?(;topicseen)?", "topic=$1." + newOffset);
                return new Topic(URI.create(newUrl), id, newOffset);
            }
        }

        public record Post(URI uri, int topicId, int postId) implements PageInfo {
            static final Pattern pattern = Pattern.compile("\\?topic=([0-9]+)\\.?([0-9]+)?(\\.msg[0-9]+)?(;topicseen)?");

            public Post(URI uri) {
                this(uri, getTopicId(uri), getPostId(uri));
            }

            public Uri asPageUri() {
                return new Uri(uri);
            }

            public Post normalize() {
                String url = uri.toString();
                url = url.replaceAll("#.*$", "");
                url = url.replaceAll("\\?PHPSESSID=[^&]+(&)?", "?");
                url = url.replaceAll("&PHPSESSID=[^&]+", "");
                url = url.replaceAll("\\?topic=([0-9]+)\\.msg([0-9]+)(;topicseen)?", "?topic=$1.msg$2");
                return new Post(URI.create(url).normalize(), topicId, postId);
            }

            static int getTopicId(URI uri) {
                Matcher matcher = pattern.matcher(uri.toString());
                if (matcher.find()) {
                    return Integer.parseInt(matcher.group(1));
                } else {
                    throw new IllegalArgumentException("Invalid topic URI: " + uri);
                }
            }

            static int getPostId(URI uri) {
                Matcher matcher = pattern.matcher(uri.toString());
                if (matcher.find()) {
                    String page = matcher.group(3);
                    return page != null ? Integer.parseInt(page) : 0;
                } else {
                    throw new IllegalArgumentException("Invalid topic URI: " + uri);
                }
            }

            static boolean isPost(URI uri) {
                return pattern.matcher(uri.toString()).find();
            }

            @Override
            public String toString() {
                return uri.toString();
            }
        }
    }
}
