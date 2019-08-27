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
import discord4j.core.object.data.stored.embed.EmbedAuthorBean;
import discord4j.core.object.data.stored.embed.EmbedBean;
import discord4j.core.object.data.stored.embed.EmbedFooterBean;
import discord4j.core.object.data.stored.embed.EmbedImageBean;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.awt.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.*;
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
 *  - Webhook support
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

    /**
     * Checks for event and news changes and updates them accordingly.
     */
    private static void check() {
        if (NEWS_CHANNEL == null || EVENT_CHANNEL == null)
            return;

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

    /**
     * Starts the periodic news/event update check.
     */
    public static void startPeriodicCheck() {
        GECkO.logger.info("Starting periodic media sync.");

        syncScheduler.scheduleWithFixedDelay(MediaSynchronizer::check, 0, UPDATE_INTERVAL_MIN, TimeUnit.MINUTES);
    }

    /**
     * Takes an {@link EmbedCreateSpec} and an {@link EmbedBean} and sets the spec to the content of the embed.
     * This is used to create a new embed based on the content of another one.
     *
     * @param embed The {@link EmbedCreateSpec} to be modified.
     * @param media The media which will be applied to the spec.
     * @return The modified {@link EmbedCreateSpec}.
     */
    private static EmbedCreateSpec setEmbedFromMedia(EmbedCreateSpec embed, EmbedBean media) {
        embed.setTitle(media.getTitle()).setDescription(media.getDescription()).setUrl(media.getUrl()).setColor(new Color(0x7289DA));

        if (media.getFooter() != null) {
            embed.setFooter(media.getFooter().getText(), null);
        }

        if (media.getAuthor() != null) {
            embed.setAuthor(media.getAuthor().getName(), media.getAuthor().getUrl(), media.getAuthor().getIconUrl());
        }

        if (media.getImage() != null) {
            embed.setImage(media.getImage().getUrl());
        }

        return embed;
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
            List<Message> newsMessages = NEWS_CHANNEL.getMessagesBefore(Snowflake.of(Instant.now().plus(10, ChronoUnit.DAYS))).take(30).collectList().block();
            if (newsMessages == null)
                return;

            Collections.reverse(newsMessages);
            newsMessages.stream().filter(message -> {
                Long postID = getPostID(message);
                // If a message has an ID, put it into the mapping
                if (postID != null) {
                    newsOrdering.add(postID);
                    MediaSynchronizer.news.put(postID, message);
                }

                return postID == null;
                // Otherwise, delete it.
            }).forEach(message -> message.delete().block());
        }

        if (EVENT_CHANNEL.getLastMessageId().isPresent()) {
            List<Message> eventMessages = EVENT_CHANNEL.getMessagesBefore(Snowflake.of(Instant.now().plus(10, ChronoUnit.DAYS))).take(30).collectList().block();
            if (eventMessages == null)
                return;

            Collections.reverse(eventMessages);
            eventMessages.stream().filter(message -> {
                Long postID = getPostID(message);
                // If a message has an ID, put it into the mapping
                if (postID != null) {
                    MediaSynchronizer.events.put(postID, message);
                }

                return postID == null;
                // Otherwise, delete it.
            }).forEach(message -> message.delete().block());
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
        if (NEWS_CHANNEL == null)
            return;

        GECkO.logger.info("Updating news channel.");

        List<News> webNews = GECkO.gecoClient.getNews(1).collectList().block();
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
                NEWS_CHANNEL.createMessage(messageCreateSpec -> messageCreateSpec.setEmbed(embedCreateSpec -> setEmbedFromMedia(embedCreateSpec, processMedia(nextWeb)))).subscribe();
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
                    EmbedBean processed = processMedia(nextWeb);
                    if (!remoteEqualsLocal(processed, nextLocal.getValue())) {
                        nextLocal.getValue().edit(messageEditSpec -> messageEditSpec.setContent(null)
                                .setEmbed(embedCreateSpec -> setEmbedFromMedia(embedCreateSpec, processed))).subscribe();

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
        if (EVENT_CHANNEL == null)
            return;

        GECkO.logger.info("Updating events channel.");

        List<Event> webEvents = GECkO.gecoClient.getEvents(1).collectList().block();
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
                events.forEach((id, message) -> message.delete().subscribe());
                events.clear();
                break;
            } else {
                eventIDs.remove(0);
            }
        }

        // Checking and Updating
        webEvents.forEach(event -> {
            EmbedBean processed = processMedia(event);
            if (events.containsKey(event.getID())) {
                if (!remoteEqualsLocal(processed, events.get(event.getID()))) {
                    events.get(event.getID()).edit(spec -> spec.setContent(null).setEmbed(embedCreateSpec -> setEmbedFromMedia(embedCreateSpec, processed))).subscribe();
                    GECkO.logger.debug("Updating local event post: {}", event.getID());
                } else {
                    GECkO.logger.debug("Event post {} is up-to-date.", event.getID());
                }
            } else {
                EVENT_CHANNEL.createMessage(spec -> spec.setContent(null).setEmbed(embedCreateSpec -> setEmbedFromMedia(embedCreateSpec, processed))).subscribe();
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
        if (NEWS_CHANNEL == null)
            return;

        if (raw) {
            processEmbedBean(message);
        }

        if (news.containsKey(id)) {
            news.get(id).edit(spec -> spec.setContent(null).setEmbed(embedCreateSpec -> setEmbedFromMedia(embedCreateSpec, message))).subscribe();
        } else {
            NEWS_CHANNEL.createMessage(spec -> spec.setContent(null).setEmbed(embedCreateSpec -> setEmbedFromMedia(embedCreateSpec, message))).subscribe(msg -> news.put(id, msg));
        }
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
        if (EVENT_CHANNEL == null)
            return;

        if (raw) {
            processEmbedBean(message);
        }

        if (events.containsKey(id)) {
            events.get(id).edit(spec -> spec.setContent(null).setEmbed(embedCreateSpec -> setEmbedFromMedia(embedCreateSpec, message))).subscribe();
        } else {
            EVENT_CHANNEL.createMessage(spec -> spec.setContent(null).setEmbed(embedCreateSpec -> setEmbedFromMedia(embedCreateSpec, message))).subscribe(msg -> events.put(id, msg));
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
     * @param id The ID of the news post to delete
     * @return A Mono which emits nothing when the news post was deleted.
     */
    public static Mono<Void> deleteNews(long id) {
        return news.remove(id).delete();
    }

    /**
     * Deletes an event post.
     *
     * @param id The ID of the event post to delete
     * @return A Mono which emits nothing when the event post was deleted.
     */
    public static Mono<Void> deleteEvent(long id) {
        return events.remove(id).delete();
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
     * @param embedBean The embed bean to process.
     * @return The processed embed bean.
     */
    private static EmbedBean processEmbedBean(EmbedBean embedBean) {
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

        String description = embedBean.getDescription();

        /* Header Parsing:
         * #      -> **__H1__**
         * ##     -> __H2__
         * ###    -> __H3__
         * ####   -> __H4__
         * #####  -> __H5__
         * ###### -> __H6__
         */

        if (description == null)
            description = "";

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
            embedBean.setImage(imageObject);
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

        // Trimming
        embedBean.setDescription(description.trim());

        // Trimming
        if (embedBean.getTitle() != null)
            embedBean.setTitle(embedBean.getTitle().trim());

        if (embedBean.getUrl() != null)
            embedBean.setUrl(embedBean.getUrl().trim());

        if (embedBean.getAuthor() != null) {
            embedBean.getAuthor().setIconUrl(BASE_URL + embedBean.getAuthor().getIconUrl()); // Extend author icon URL
            embedBean.getAuthor().setIconUrl(embedBean.getAuthor().getIconUrl().trim());
            embedBean.getAuthor().setName(embedBean.getAuthor().getName().trim());
            embedBean.getAuthor().setUrl(embedBean.getAuthor().getUrl().trim());
        }

        if (embedBean.getFooter() != null) {
            embedBean.getFooter().setText(embedBean.getFooter().getText().trim());
        }

        if (embedBean.getImage() != null) {
            embedBean.getImage().setUrl(embedBean.getImage().getUrl().trim());
        }

        // Description length trimming
        if (embedBean.getDescription().length() > Embed.MAX_DESCRIPTION_LENGTH) {
            String trimString = "...\n\n[Read more](" + embedBean.getUrl() + ")";
            embedBean.setDescription(embedBean.getDescription().substring(0, Embed.MAX_DESCRIPTION_LENGTH - trimString.length()) + trimString);
        }

        return embedBean;
    }

    /**
     * Processes all the markdown stuff not supported by discord and tries to replace it as good as possible.
     *
     * @param event The remote event post to process.
     * @return The processed embed bean.
     */
    private static EmbedBean processMedia(Event event) {
        EmbedBean processed = new EmbedBean();
        processed.setTitle(event.getTitle());
        processed.setUrl(event.getURL());
        processed.setDescription(event.getDescription());

        return processEmbedBean(processed);
    }

    /**
     * Processes all the markdown stuff not supported by discord and tries to replace it as good as possible.
     *
     * @param news The remote news post to process.
     * @return The processed embed bean.
     */
    private static EmbedBean processMedia(News news) {
        EmbedBean processed = new EmbedBean();
        processed.setTitle(news.getTitle());
        processed.setUrl(news.getURL());
        processed.setDescription(news.getDescription());

        EmbedAuthorBean author = new EmbedAuthorBean();
        author.setName(news.getAuthorName());
        author.setUrl(news.getAuthorURL());
        author.setIconUrl(news.getAuthorIconURL());
        processed.setAuthor(author);

        EmbedFooterBean footer = new EmbedFooterBean();
        footer.setText(news.getFooter());
        processed.setFooter(footer);

        return processEmbedBean(processed);
    }

    /**
     * Checks if a remote news post is equal to a message containing a local news post.
     *
     * @param remote The remote media post in form of an already processed embed bean.
     * @param local  The local media post in form of the message containing it.
     * @return True if they are equal, otherwise false.
     */
    private static boolean remoteEqualsLocal(EmbedBean remote, Message local) {
        if (local.getEmbeds().size() != 1) {
            return false;
        }

        Embed embed = local.getEmbeds().get(0);

        // Check author properties
        if ((!embed.getAuthor().isPresent() && remote.getAuthor() != null) || (embed.getAuthor().isPresent() && remote.getAuthor() == null))
            return false;
        if (embed.getAuthor().isPresent()) {
            Embed.Author localAuthor = embed.getAuthor().get();
            EmbedAuthorBean remoteAuthor = remote.getAuthor();
            if ((localAuthor.getName() != null && remoteAuthor.getName() == null) || (localAuthor.getName() == null && remoteAuthor.getName() != null))
                return false;
            if (localAuthor.getName() != null && !localAuthor.getName().equals(remoteAuthor.getName())) {
                GECkO.logger.debug("Author name has changed.");
                return false;
            }

            if ((localAuthor.getUrl() != null && remoteAuthor.getUrl() == null) || (localAuthor.getUrl() == null && remoteAuthor.getUrl() != null))
                return false;
            if (localAuthor.getUrl() != null && !localAuthor.getUrl().equals(remote.getAuthor().getUrl())) {
                GECkO.logger.debug("Author url has changed.");
                return false;
            }

            if ((localAuthor.getIconUrl() != null && remoteAuthor.getIconUrl() == null) || (localAuthor.getIconUrl() == null && remoteAuthor.getIconUrl() != null))
                return false;
            if (localAuthor.getIconUrl() != null && !localAuthor.getIconUrl().equals(remoteAuthor.getIconUrl())) {
                GECkO.logger.debug("Author icon url has changed.");
                return false;
            }
        }

        // Check title
        if ((!embed.getTitle().isPresent() && remote.getTitle() != null) || (embed.getTitle().isPresent() && remote.getTitle() == null))
            return false;
        if (embed.getTitle().isPresent() && !embed.getTitle().get().equals(remote.getTitle())) {
            GECkO.logger.debug("Title has changed.");
            return false;
        }

        // Check URL
        if ((!embed.getUrl().isPresent() && remote.getUrl() != null) || (embed.getUrl().isPresent() && remote.getUrl() == null))
            return false;
        if (embed.getUrl().isPresent() && !embed.getUrl().get().equals(remote.getUrl())) {
            GECkO.logger.debug("URL has changed.");
            return false;
        }

        // Check description
        if ((!embed.getDescription().isPresent() && remote.getDescription() != null) || (embed.getDescription().isPresent() && remote.getDescription() == null))
            return false;
        if (embed.getDescription().isPresent() && !embed.getDescription().get().equals(remote.getDescription())) {
            GECkO.logger.debug("Description has changed.");
            return false;
        }

        // Check footer
        if ((!embed.getFooter().isPresent() && remote.getFooter() != null) || (embed.getFooter().isPresent() && remote.getFooter() == null))
            return false;
        if (embed.getFooter().isPresent()) {
            Embed.Footer localFooter = embed.getFooter().get();
            EmbedFooterBean remoteFooter = remote.getFooter();
            if ((localFooter.getText() != null && remoteFooter.getText() == null) || (localFooter.getText() == null && remoteFooter.getText() != null))
                return false;
            if (localFooter.getText() != null && !localFooter.getText().equals(remoteFooter.getText())) {
                GECkO.logger.debug("Footer has changed.");
                return false;
            }
        }

        // Check images
        if ((!embed.getImage().isPresent() && remote.getImage() != null) || (embed.getImage().isPresent() && remote.getImage() == null))
            return false;
        if (embed.getImage().isPresent()) {
            Embed.Image localImage = embed.getImage().get();
            EmbedImageBean remoteImage = remote.getImage();
            if ((localImage.getUrl() != null && remoteImage.getUrl() == null) || (localImage.getUrl() == null && remoteImage.getUrl() != null))
                return false;
            if (localImage.getUrl() != null && !localImage.getUrl().equals(remoteImage.getUrl())) {
                GECkO.logger.debug("Image has changed.");
                return false;
            }
        }

        return true;
    }
}
