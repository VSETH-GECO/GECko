/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
 */

package ch.ethz.geco.gecko;

import ch.ethz.geco.g4j.obj.Event;
import ch.ethz.geco.g4j.obj.News;
import discord4j.core.object.Embed;
import discord4j.core.object.data.stored.embed.EmbedBean;
import discord4j.core.object.data.stored.embed.EmbedImageBean;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Implementation help:
 *  - After restart
 *      1)      Read existing entries and cleanup
 *  - Periodic checking and updating
 *      1)      Load news from website (all or last page?)
 *      2)      Read existing entries
 *      3)      Compare and edit entries
 *  - Websocket support
 *      - Support updating, deletion and creation
 */

/**
 * Synchronizes the news and events from the website with the corresponding channels. <p>
 * See {@link ch.ethz.geco.gecko.rest.WebHookServer} for details.
 */
public class MediaSynchronizer {
    private static final String BASE_URL = "https://geco.ethz.ch";
    private static final TextChannel NEWS_CHANNEL = GECkO.discordClient.getChannelById(Snowflake.of(ConfigManager.getProperties().getProperty("media_newsChannelID"))).ofType(TextChannel.class).block();
    private static final TextChannel EVENT_CHANNEL = GECkO.discordClient.getChannelById(Snowflake.of(ConfigManager.getProperties().getProperty("media_eventChannelID"))).ofType(TextChannel.class).block();

    private static final ArrayList<Long> newsOrdering = new ArrayList<>();
    private static final LinkedHashMap<Long, Message> news = new LinkedHashMap<>();
    private static final LinkedHashMap<Long, Message> events = new LinkedHashMap<>();

    private static final Pattern idPattern = Pattern.compile("^\\s*https?://(?:www.)?geco.ethz.ch/(?:news|events)/(\\d+)/?\\s*$");

    private static final ScheduledExecutorService syncScheduler = Executors.newSingleThreadScheduledExecutor();
    private static final Integer UPDATE_INTERVAL_MIN = 10;

    private static void check() {
        try {
            if (!GECkO.discordClient.isConnected())
                return;

            init();
            loadNews();
            loadEvents();
        } catch (Exception e) {
            // Catch all exceptions to prevent the timer from stopping when an unchecked exception occurs
            e.printStackTrace();
        }
    }

    public static void startPeriodicCheck() {
        GECkO.logger.info("Starting periodic media sync.");

        syncScheduler.scheduleWithFixedDelay(MediaSynchronizer::check, 0, UPDATE_INTERVAL_MIN, TimeUnit.MINUTES);
    }

    private static EmbedCreateSpec setEmbedFromMedia(EmbedCreateSpec embed, News news) {
        return embed.setTitle(news.getTitle()).setDescription(news.getDescription()).setUrl(news.getURL())
                .setColor(new Color(0x7289DA)).setFooter(news.getFooter(), null)
                .setAuthor(news.getAuthorName(), news.getAuthorURL(), news.getAuthorIconURL());
    }

    private static EmbedCreateSpec setEmbedFromMedia(EmbedCreateSpec embed, Event event) {
        return embed.setTitle(event.getTitle()).setDescription(event.getDescription()).setUrl(event.getURL()).setColor(new Color(0x7289DA));
    }

    /**
     * Gets the ID of a news or event post. If the given message does not contain a news
     * or event post, this function will return null.
     *
     * @param message The message which possibly contains a news or event post.
     * @return The ID of the news or event post, if existing.
     */
    private static Long getPostID(Message message) {
        List<Embed> embeds = message.getEmbeds();

        // A valid news or event entry has exactly one embed.
        if (embeds.size() != 1 || !embeds.get(0).getUrl().isPresent()) {
            return null;
        }

        // Return if the url of the embed matches with the url of a news or event entry.
        Matcher matcher = idPattern.matcher(embeds.get(0).getUrl().get());
        if (matcher.matches()) {
            return Long.valueOf(matcher.group(1));
        }

        return null;
    }

    /**
     * Reads in all existing news and event entries and removes invalid entries.
     */
    public static void init() {
        if (NEWS_CHANNEL == null || EVENT_CHANNEL == null)
            return;

        // Reinitialize ID to post mappings
        MediaSynchronizer.news.clear();
        MediaSynchronizer.newsOrdering.clear();

        if (NEWS_CHANNEL.getLastMessageId().isPresent()) {
            NEWS_CHANNEL.getMessagesBefore(NEWS_CHANNEL.getLastMessageId().get()).take(30).filter(message -> {
                Long postID = getPostID(message);
                // If a message has an ID, put it into the mapping
                if (postID != null) {
                    newsOrdering.add(postID);
                    MediaSynchronizer.news.put(postID, message);
                }

                return postID == null;
                // Otherwise, delete it.
            }).map(message -> message.delete().block()).then().block();
        }

        if (EVENT_CHANNEL.getLastMessageId().isPresent()) {
            EVENT_CHANNEL.getMessagesBefore(EVENT_CHANNEL.getLastMessageId().get()).take(30).filter(message -> {
                Long postID = getPostID(message);
                // If a message has an ID, put it into the mapping
                if (postID != null) {
                    MediaSynchronizer.events.put(postID, message);
                }

                return postID == null;
                // Otherwise, delete it.
            }).map(message -> message.delete().block()).then().block();
        }

        GECkO.logger.debug("Found {} local news and {} local event posts.", news.size(), events.size());

        StringBuilder newsPosts = new StringBuilder();
        news.forEach((key, value) -> newsPosts.append(key).append(" "));

        GECkO.logger.debug("News posts: {}", newsPosts.toString());

        StringBuilder eventPosts = new StringBuilder();
        events.forEach((key, value) -> eventPosts.append(key).append(" "));

        GECkO.logger.debug("Event posts: {}", eventPosts.toString());
    }

    public static void loadNews() {
        if (NEWS_CHANNEL == null || EVENT_CHANNEL == null)
            return;

        GECkO.logger.info("Updating news channel.");

        List<News> webNews = GECkO.gecoClient.getNews(1);
        if (webNews == null) {
            return;
        }

        GECkO.logger.debug("Found {} remote news posts.", webNews.size());

        if (webNews.isEmpty()) {
            GECkO.logger.warn("This is very unlikely to happen. Possible Bug!");
            return;
        }

        // Sort ascending for sanity check
        webNews.sort(Comparator.comparing(News::getPublishedAt));

        // Store all available news post IDs
        List<Long> webIDs = new ArrayList<>();
        webNews.forEach(post -> webIDs.add(post.getID()));

        // Remove drafts from web news
        webNews.removeIf(News::isDraft);

        // Remove all posts missing remotely after the first web post
        GECkO.logger.debug("Deleting drafts and deleted posts.");
        if (newsOrdering.contains(webIDs.get(0))) {
            news.forEach((id, post) -> {
                if (newsOrdering.indexOf(id) >= newsOrdering.indexOf(webIDs.get(0)) && !webIDs.contains(id)) {
                    GECkO.logger.debug("Deleting news post: {}", id);
                    post.delete().block();
                }
            });

            news.entrySet().removeIf(post -> newsOrdering.indexOf(post.getKey()) >= newsOrdering.indexOf(webIDs.get(0)) && !webIDs.contains(post.getKey()));
        }

        // Check proper ordering of local posts
        // TODO: Might break if posts are missing locally in the middle
        GECkO.logger.debug("Checking for wrongly ordered posts.");
        Iterator<Map.Entry<Long, Message>> localNewsIterator = news.entrySet().iterator();
        boolean foundFirst = false;
        while (localNewsIterator.hasNext() && !webIDs.isEmpty()) {
            Map.Entry<Long, Message> next = localNewsIterator.next();

            if (!foundFirst && next.getKey().equals(webIDs.get(0))) {
                webIDs.remove(0);
                foundFirst = true;
                continue;
            }

            if (foundFirst) {
                if (!next.getKey().equals(webIDs.get(0))) {
                    GECkO.logger.debug("News post {} is wrongly ordered, removing...", next.getKey());
                    next.getValue().delete().block();
                    localNewsIterator.remove();
                } else {
                    webIDs.remove(0);
                }
            }
        }

        boolean stay = false;
        Map.Entry<Long, Message> nextLocal = null;
        localNewsIterator = news.entrySet().iterator();
        for (News nextWeb : webNews) {
            GECkO.logger.debug("Searching local news post: {}", nextWeb.getID());

            // If there are no more local posts, but there are still web posts missing.
            if (!localNewsIterator.hasNext()) {
                GECkO.logger.debug("Posting new news post: {}", nextWeb.getID());
                NEWS_CHANNEL.createMessage(messageCreateSpec -> messageCreateSpec.setEmbed(embedCreateSpec -> setEmbedFromMedia(embedCreateSpec, nextWeb))).subscribe();
            }

            while (localNewsIterator.hasNext() || stay) {
                if (!stay) {
                    nextLocal = localNewsIterator.next();
                }

                if (newsOrdering.contains(webNews.get(0).getID()) && newsOrdering.indexOf(nextLocal.getKey()) < newsOrdering.indexOf(webNews.get(0).getID())) {
                    // If we are behind, we skip until we find a news post
                    GECkO.logger.debug("Found old news post: {}", nextLocal.getKey());
                    stay = false;
                } else if (nextWeb.getID().equals(nextLocal.getKey())) {
                    GECkO.logger.debug("Checking local news post: {}", nextLocal.getKey());
                    EmbedObject processed = processEmbed(getEmbedFromMedia(nextWeb));
                    if (!embedEqualsEmbedObject(nextLocal.getValue().getEmbeds().get(0), processed)) {
                        nextLocal.getValue().edit(messageEditSpec -> messageEditSpec.setContent(null)
                                .setEmbed(embedCreateSpec -> setEmbedFromMedia(embedCreateSpec, nextWeb))).subscribe();

                        GECkO.logger.debug("Updating local news post: {}", nextLocal.getKey());
                    } else {
                        GECkO.logger.debug("News post {} is up-to-date.", nextLocal.getKey());
                    }
                    stay = false;
                    break;
                } else {
                    GECkO.logger.warn("News post {} is missing locally, ignoring.", nextWeb.getID());
                    stay = true;
                    break;
                }
            }
        }
    }

    public static void loadEvents() {
        GECkO.logger.info("Updating events channel.");

        List<Event> webEvents = GECkO.gecoClient.getEvents(1);
        if (webEvents == null) {
            return;
        }

        GECkO.logger.debug("Found {} remote event posts.", webEvents.size());

        // Store all available event post IDs
        List<Long> eventIDs = new ArrayList<>();
        webEvents.forEach(event -> eventIDs.add(event.getID()));

        GECkO.logger.debug("Deleting deleted events.");
        events.forEach((id, message) -> {
            if (!eventIDs.contains(id)) {
                message.delete().block();
            }
        });
        events.entrySet().removeIf(entry -> !eventIDs.contains(entry.getKey()));

        // Check proper ordering of local posts
        GECkO.logger.debug("Checking for wrongly ordered posts.");
        Iterator<Map.Entry<Long, Message>> localEventsIterator = events.entrySet().iterator();
        while (localEventsIterator.hasNext() && !eventIDs.isEmpty()) {
            Map.Entry<Long, Message> next = localEventsIterator.next();

            if (!next.getKey().equals(eventIDs.get(0))) {
                GECkO.logger.info("Found order inconsistencies in events channel, wiping...");
                events.forEach((id, message) -> RequestBuffer.request(message::delete).get());
                events.clear();
                break;
            } else {
                eventIDs.remove(0);
            }
        }

        // Checking and Updating
        webEvents.forEach(event -> {
            EmbedObject processed = processEmbed(getEmbedFromMedia(event));
            if (events.containsKey(event.getID())) {
                if (!embedEqualsEmbedObject(events.get(event.getID()).getEmbeds().get(0), processed)) {
                    RequestBuffer.request(() -> events.get(event.getID()).edit(processed)).get();
                    GECkO.logger.debug("Updating local event post: {}", event.getID());
                } else {
                    GECkO.logger.debug("Event post {} is up-to-date.", event.getID());
                }
            } else {
                RequestBuffer.request(() -> EVENT_CHANNEL.sendMessage(processed)).get();
                GECkO.logger.debug("Posting new event post: {}", event.getID());
            }
        });
    }

    /**
     * Adds or edits (if already existing) a news post. If raw is true, the description will be parsed to be compatible with discord.
     *
     * @param id      the ID of the news post
     * @param message the embed to update or add
     * @param raw     if it should be parsed for discord or not
     */
    private static void setNews(long id, EmbedBean message, boolean raw) {
        if (raw) {
            processEmbed(message);
        }

        final EmbedObject finalMsg = message;

        RequestBuffer.request(() -> {
            if (news.containsKey(id)) {
                news.get(id).edit(finalMsg);
            } else {
                news.put(id, NEWS_CHANNEL.sendMessage(finalMsg));
            }
        }).get();
    }

    /**
     * Adds or edits (if already existing) a news post. It will parse the message content to make it compatible with discord.
     *
     * @param id      the ID of the news post
     * @param message the embed to update or add
     */
    public static void setNews(int id, EmbedBean message) {
        setNews(id, message, true);
    }

    /**
     * Adds or edits (if already existing) an event post. If raw is true, the description will be parsed to be compatible with discord.
     *
     * @param id      the ID of the event post
     * @param message the embed to update or add
     * @param raw     if it should be parsed for discord or not
     */
    private static void setEvent(long id, EmbedBean message, boolean raw) {
        if (raw) {
            processEmbed(message);
        }

        if (events.containsKey(id)) {
            events.get(id).edit(message);
        } else {
            events.put(id, EVENT_CHANNEL.sendMessage(message));
        }
    }

    /**
     * Adds or edits (if already existing) an event post. It will parse the message content to make it compatible with discord.
     *
     * @param id      the ID of the event post
     * @param message the embed to update or add
     */
    public static void setEvent(long id, EmbedBean message) {
        setEvent(id, message, true);
    }

    /**
     * Deletes a news post.
     *
     * @param id the ID of the news post to delete
     */
    public static void deleteNews(long id) {
        news.remove(id).delete();
    }

    /**
     * Deletes an event post.
     *
     * @param id the ID of the event post to delete
     */
    public static void deleteEvent(long id) {
        events.remove(id).delete();
    }

    // Pre-compile all needed patterns
    private static final Pattern headerPattern = Pattern.compile("(#+)\\s*([^\\r\\n]+)(?>\\r\\n|\\r|\\n|$)");
    private static final Pattern imagePattern = Pattern.compile("!\\[[^]]*]\\(([^)]*)\\)");
    private static final Pattern nestedPattern = Pattern.compile("\\[!\\[([^]]*)]\\([^)]*\\)]\\(([^)]*)\\)");
    private static final Pattern emptyLinkPattern = Pattern.compile("\\[[^]]*]\\(\\s*\\)");
    private static final Pattern emptyLinkNamePattern = Pattern.compile("\\[\\s*]\\(([^)]*)\\)");
    private static final Pattern iconPattern = Pattern.compile("\\{\\{[^}]+}}");
    private static final Pattern iframePattern = Pattern.compile("\\{iframe}\\(([^)]*)\\)");
    private static final Pattern youtubePattern = Pattern.compile("https?://(?:www\\.)?youtu(?:be\\.com/watch\\?v=|\\.be/)([\\w\\-_]{1,11})");

    /**
     * Processes all the markdown stuff not supported by discord and tries to replace it as good as possible.
     *
     * @param raw the embed with unprocessed description
     * @return the processed embed
     */
    private static EmbedBean processEmbed(EmbedBean raw) {
        /* Markdown Processing:
         * - Emphasis:  supported
         * ->Header:    not supported and has to be parsed
         * - List:      not supported but left as is
         * - Link:      supported
         * - Quote:     not supported but left as is
         * ->Image:     not supported and has to be removed
         * - Table:     not supported but left as is
         *              TODO: could be done as ASCII version inside a code block
         * - Code:      supported
         *
         * Extension:
         * ->Text Highlighting: not supported and has to be parsed
         * - Horizontal Line:   not supported but left as is
         *                      TODO: could be done as empty code block or embed fields?
         * ->Icon:              not supported and has to be removed
         * ->IFrame:            not supported but has to be parsed
         */

        String description = raw.getDescription();

        /* Header Parsing:
         * #      -> **__H1__**
         * ##     -> __H2__
         * ###    -> __H3__
         * ####   -> __H4__
         * #####  -> __H5__
         * ###### -> __H6__
         */

        Matcher headerMatcher = headerPattern.matcher(description);

        while (headerMatcher.find()) {
            int heading = headerMatcher.group(1).length();

            if (heading == 1) {
                description = headerMatcher.replaceFirst("**__$2__**\n");
            } else {
                description = headerMatcher.replaceFirst("__$2__\n");
            }

            // Reset matcher
            headerMatcher = headerPattern.matcher(description);
        }

        /* (Nested) Image Parsing:
         * 1. Search for an image to use as message image
         * 2. Resolve all nested image links
         * 2.1 By using the image description as the link name instead of the image
         * 3. Remove all remaining images TODO: Maybe post them as links?
         * 4. Fix broken links
         */

        Matcher imageMatcher = imagePattern.matcher(description);

        // Search for an image to use as message image
        if (imageMatcher.find()) {
            EmbedImageBean imageObject = new EmbedImageBean();
            imageObject.setUrl(imageMatcher.group(1));
            raw.setImage(imageObject);
        }

        // Replace all nested image links with normal ones

        Matcher nestedMatcher = nestedPattern.matcher(description);
        description = nestedMatcher.replaceAll("[$1]($2)");

        // Remove remaining images
        imageMatcher = imagePattern.matcher(description);
        description = imageMatcher.replaceAll("");

        // Remove empty links
        description = emptyLinkPattern.matcher(description).replaceAll("");

        // Fix links with no link name
        description = emptyLinkNamePattern.matcher(description).replaceAll("$1");

        /* Text Highlighting Parsing:
         * 1. "Simply" replace with bold
         *
         * Since we can't exclude words from matching ("==([^(==)]*)==" does not work), we will implement
         * our own matching algorithm.
         */

        char lastChar = '\0';
        int quoteBegin = -1;
        for (int i = 0; i < description.length(); i++) {
            char curChar = description.charAt(i);
            // If we found a potential start or end
            if (curChar == '=' && lastChar == '=') {
                // Check if quote begin is already set
                if (quoteBegin == -1) {
                    quoteBegin = i - 1;
                } else { // If not, it must be a quote end
                    int quoteEnd = i + 1;

                    // Replace highlight with bold
                    description = description.substring(0, quoteBegin) + "**" + description.substring(quoteBegin + 2, quoteEnd - 2) + "**" + description.substring(quoteEnd);
                    quoteBegin = -1; // Reset quote begin
                }
            }

            lastChar = curChar;
        }

        /* Icon parsing:
         * Icons will be removed until a better solution has been found TODO: emojis?
         */

        Matcher iconMatcher = iconPattern.matcher(description);
        description = iconMatcher.replaceAll("");

        /* IFrame parsing:
         * - Change YouTube IFrame to link
         * - Parse Google Maps IFrame to link
         */

        Matcher iFrameMatcher = iframePattern.matcher(description);
        while (iFrameMatcher.find()) {
            if (iFrameMatcher.group(1).length() == 0) {
                description = iFrameMatcher.replaceFirst("");
            } else {
                Matcher youtubeMatcher = youtubePattern.matcher(iFrameMatcher.group());
                if (youtubeMatcher.find()) {
                    description = iFrameMatcher.replaceFirst("[Video](" + youtubeMatcher.group() + ")");
                } else {
                    // TODO: also parse google maps
                    description = iFrameMatcher.replaceFirst("");
                }
            }

            iFrameMatcher = iframePattern.matcher(description);
        }

        // Extend author icon URL
        if (raw.getAuthor() != null) {
            raw.getAuthor().setIconUrl(BASE_URL + raw.getAuthor().getIconUrl());
        }

        // Trimming
        raw.setDescription(description.trim());
        raw.setTitle(raw.getTitle().trim());
        raw.setUrl(raw.getUrl().trim());

        if (raw.getAuthor() != null) {
            raw.getAuthor().setIconUrl(raw.getAuthor().getIconUrl().trim());
            raw.getAuthor().setName(raw.getAuthor().getName().trim());
            raw.getAuthor().setUrl(raw.getAuthor().getUrl().trim());
        }

        if (raw.getFooter() != null) {
            raw.getFooter().setText(raw.getFooter().getText().trim());
        }

        if (raw.getImage() != null) {
            raw.getImage().setUrl(raw.getImage().getUrl().trim());
        }

        // Description length trimming
        if (raw.getDescription().length() > Embed.MAX_DESCRIPTION_LENGTH) {
            String trimString = "...\n\n[Read more](" + raw.getUrl() + ")";
            raw.setDescription(raw.getDescription().substring(0, Embed.MAX_DESCRIPTION_LENGTH - trimString.length()) + trimString);
        }

        return raw;
    }

    /**
     * Returns whether or not the given embeds are the same in terms of a news or event post.
     *
     * @param e1 the first embed
     * @param e2 the second embed
     * @return if the two embeds are equal
     */
    private static boolean embedEqualsEmbedObject(IEmbed e1, EmbedObject e2) {
        IEmbed.IEmbedAuthor author1 = e1.getAuthor();
        EmbedObject.AuthorObject author2 = e2.author;
        if ((author1 == null) == (author2 == null)) {
            if (author1 != null) {
                if (!Objects.equals(author1.getIconUrl(), author2.icon_url)) {
                    GECkO.logger.debug("Author icon URL has changed.");
                    return false;
                }

                if (!Objects.equals(author1.getName(), author2.name)) {
                    GECkO.logger.debug("Author name has changed.");
                    return false;
                }

                if (!Objects.equals(author1.getUrl(), author2.url)) {
                    GECkO.logger.debug("Author URL has changed.");
                    return false;
                }
            }
        } else {
            return false;
        }

        if (!Objects.equals(e1.getTitle(), e2.title)) {
            GECkO.logger.debug("Title has changed.");
            return false;
        }

        if (!Objects.equals(e1.getUrl(), e2.url)) {
            GECkO.logger.debug("URL has changed.");
            return false;
        }

        if (!Objects.equals(e1.getDescription(), e2.description)) {
            if (!(e1.getDescription() == null && Objects.equals(e2.description, ""))) {
                GECkO.logger.debug("Description has changed.");
                return false;
            }
        }

        IEmbed.IEmbedFooter footer1 = e1.getFooter();
        EmbedObject.FooterObject footer2 = e2.footer;
        if ((footer1 == null) == (footer2 == null)) {
            if (footer1 != null) {
                if (!Objects.equals(footer1.getText(), footer2.text)) {
                    GECkO.logger.debug("Footer text has changed.");
                    return false;
                }
            }
        } else {
            return false;
        }

        IEmbed.IEmbedImage image1 = e1.getImage();
        EmbedObject.ImageObject image2 = e2.image;
        if ((image1 == null) == (image2 == null)) {
            if (image1 != null) {
                if (!Objects.equals(image1.getUrl(), image2.url)) {
                    GECkO.logger.debug("Image URL has changed.");
                    return false;
                }
            }
        } else {
            return false;
        }

        return true;
    }
}
