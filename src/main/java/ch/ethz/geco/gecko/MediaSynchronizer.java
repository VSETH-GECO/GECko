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

import ch.ethz.geco.gecko.rest.RequestBuilder;
import ch.ethz.geco.gecko.rest.api.exception.APIException;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import sx.blah.discord.api.internal.DiscordUtils;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IEmbed;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.RequestBuffer;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Synchronizes the news and events from the website with the corresponding channels. <p>
 * See {@link ch.ethz.geco.gecko.rest.WebHookServer} for details.
 */
public class MediaSynchronizer {
    private static final String API_URL = "https://geco.ethz.ch";
    private static final IChannel newsChannel = GECkO.discordClient.getChannelByID(Long.valueOf(ConfigManager.getProperties().getProperty("media_newsChannelID")));
    private static final IChannel eventChannel = GECkO.discordClient.getChannelByID(Long.valueOf(ConfigManager.getProperties().getProperty("media_eventChannelID")));

    private static final Map<Integer, IMessage> news = new HashMap<>();
    private static final Map<Integer, IMessage> events = new HashMap<>();

    private static final Pattern idPattern = Pattern.compile("/(\\d+)/?");

    /**
     * Loads the news from the website. This should only be used once every restart.
     * It will load last page of news and check for every existing news entry if it's up-to-date.
     */
    public static void loadNews() {
        try {
            HttpResponse response = new RequestBuilder(API_URL + "/api/v2/web/news")
                    .addHeader("X-API-KEY", ConfigManager.getProperties().getProperty("geco_apiKey"))
                    .get();

            StatusLine statusLine = response.getStatusLine();
            //Header encoding = response.getEntity().getContentEncoding();
            //String content = IOUtils.toString(response.getEntity().getContent(), encoding.getValue() != null ? encoding.getValue() : "UTF-8");

            switch (statusLine.getStatusCode()) {
                case 200:
                    // Directly map json to embed objects
                    List<EmbedObject> webEmbeds = DiscordUtils.MAPPER.readValue(response.getEntity().getContent(), DiscordUtils.MAPPER.getTypeFactory().constructCollectionType(List.class, EmbedObject.class));
                    DualHashBidiMap<Integer, EmbedObject> webEmbedMap = new DualHashBidiMap<>();

                    // Sort embeds by news ID or remove them if they don't have a news ID
                    webEmbeds.sort(Comparator.comparing(embedObject -> {
                        Matcher idMatcher = idPattern.matcher(embedObject.url);

                        if (idMatcher.find()) {
                            int id = Integer.parseInt(idMatcher.group(1));
                            webEmbedMap.put(id, embedObject);
                            return id;
                        } else {
                            // Don't add elements without IDs
                            GECkO.logger.warn("Received news post without ID. Contact the Web Master!");
                            return 0;
                        }
                    }));

                    // Remove all embeds which don't have an ID
                    webEmbeds.removeIf(embedObject -> !webEmbedMap.containsValue(embedObject));

                    // Get the last 20 messages
                    List<IMessage> newsHistory = newsChannel.getMessageHistory(20);
                    GECkO.logger.debug("Found {} messages in the news channel.", newsHistory.size());

                    // Load existing news posts
                    GECkO.logger.debug("Loading existing news posts...");
                    for (IMessage message : newsHistory) {
                        if (message.getEmbeds() != null && message.getEmbeds().size() > 0) {
                            IEmbed embed = message.getEmbeds().get(0);
                            Matcher idMatcher = idPattern.matcher(embed.getUrl());

                            if (idMatcher.find()) {
                                int id = Integer.parseInt(idMatcher.group(1));

                                if (webEmbedMap.containsKey(id)) {
                                    news.put(id, message);
                                    continue;
                                } else {
                                    GECkO.logger.debug("Removing news post without corresponding web embed");
                                }
                            } else {
                                GECkO.logger.debug("Removing message without ID.");
                            }
                        } else {
                            GECkO.logger.debug("Removing message without embed.");
                        }

                        // Delete message of news post if it doesn't have an ID or no corresponding web embed
                        message.delete();
                    }
                    GECkO.logger.debug("Loaded {} existing news posts.", news.size());

                    // Checking for missing news posts and update existing ones
                    int firstMissing = -1;
                    int lastFound = -1;
                    for (EmbedObject embedObject : webEmbeds) {
                        // If there is already a news post corresponding to the current web embed
                        int curID = webEmbedMap.getKey(embedObject);

                        IMessage newsMessage = news.get(curID);
                        if (newsMessage != null) {
                            lastFound = curID;

                            final EmbedObject processedEmbed = processMarkdown(extendAuthorIconUrl(embedObject));
                            if (!embedEqualsEmbedObject(newsMessage.getEmbeds().get(0), processedEmbed)) {
                                GECkO.logger.debug("Updating news post: {}", curID);
                                RequestBuffer.request(() -> {
                                    newsMessage.edit(processedEmbed);
                                }).get();
                            } else {
                                GECkO.logger.debug("News post {} is up-to-date.", curID);
                            }
                        } else {
                            if (firstMissing == -1) {
                                firstMissing = curID;
                            }
                        }
                    }

                    // Fix missing posts
                    for (EmbedObject embedObject : webEmbeds) {
                        int curID = webEmbedMap.getKey(embedObject);

                        // If it's not already in the news channel
                        if (!news.containsKey(curID)) {
                            // If the missing post is before already existing posts, we can't post it since we
                            // can't post messages before other messages.
                            if (lastFound > curID) {
                                GECkO.logger.warn("Missing news post {} in between. Can't fix without breaking reactions. Ignoring.", curID);
                            } else {
                                GECkO.logger.debug("Posting missing news post with ID: {}", curID);
                                final EmbedObject processedEmbed = processMarkdown(extendAuthorIconUrl(embedObject));
                                RequestBuffer.request(() -> {
                                    newsChannel.sendMessage(processedEmbed);
                                }).get();
                            }
                        }
                    }
                    break;
                case 400: // Bad Request
                    ErrorHandler.handleError(new APIException("Bad Request"));
                    break;
                case 401: // Unauthorized
                    ErrorHandler.handleError(new APIException("Bot is unauthorized."));
                    break;
                case 403: // Forbidden
                    ErrorHandler.handleError(new APIException("Forbidden"));
                    break;
                case 404: // Not Found
                    ErrorHandler.handleError(new APIException("Not found"));
                    break;
                case 500: // Internal Server Error
                    ErrorHandler.handleError(new APIException("Internal Server Error"));
                    break;
            }
        } catch (IOException e) {
            ErrorHandler.handleError(e);
        }
    }

    /**
     * Adds or edits (if already existing) a news post. If raw is true, the description will be parsed to be compatible with discord.
     *
     * @param id      the ID of the news post
     * @param message the embed to update or add
     * @param raw     if it should be parsed for discord or not
     */
    public static void setNews(int id, EmbedObject message, boolean raw) {
        if (raw) {
            message = processMarkdown(extendAuthorIconUrl(message));
        }

        final EmbedObject finalMsg = message;

        RequestBuffer.request(() -> {
            if (news.containsKey(id)) {
                news.get(id).edit(finalMsg);
            } else {
                news.put(id, newsChannel.sendMessage(finalMsg));
            }
        }).get();
    }

    /**
     * Adds or edits (if already existing) a news post. It will parse the message content to make it compatible with discord.
     *
     * @param id      the ID of the news post
     * @param message the embed to update or add
     */
    public static void setNews(int id, EmbedObject message) {
        setNews(id, message, true);
    }

    /**
     * Adds or edits (if already existing) an event post. If raw is true, the description will be parsed to be compatible with discord.
     *
     * @param id      the ID of the event post
     * @param message the embed to update or add
     * @param raw     if it should be parsed for discord or not
     */
    public static void setEvent(int id, EmbedObject message, boolean raw) {
        if (raw) {
            message = processMarkdown(extendAuthorIconUrl(message));
        }

        // Set message color
        message.color = 7506394; // Discord blue

        if (events.containsKey(id)) {
            events.get(id).edit(message);
        } else {
            events.put(id, eventChannel.sendMessage(message));
        }
    }

    /**
     * Adds or edits (if already existing) an event post. It will parse the message content to make it compatible with discord.
     *
     * @param id      the ID of the event post
     * @param message the embed to update or add
     */
    public static void setEvent(int id, EmbedObject message) {
        setEvent(id, message, true);
    }

    /**
     * Deletes a news post.
     *
     * @param id the ID of the news post to delete
     */
    public static void deleteNews(int id) {
        news.remove(id).delete();
    }

    /**
     * Deletes an event post.
     *
     * @param id the ID of the event post to delete
     */
    public static void deleteEvent(int id) {
        events.remove(id).delete();
    }

    /**
     * Extends the author icon url to the absolute address instead of relative address.
     *
     * @param raw the embed to process
     * @return the embed with fixed icon url.
     */
    private static EmbedObject extendAuthorIconUrl(EmbedObject raw) {
        raw.author.icon_url = API_URL + raw.author.icon_url;
        return raw;
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
    public static EmbedObject processMarkdown(EmbedObject raw) {
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
         *                      TODO: could be done as empty code block?
         * ->Icon:              not supported and has to be removed
         * ->IFrame:            not supported but has to be parsed
         */

        String description = raw.description;

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
            EmbedObject.ImageObject imageObject = new EmbedObject.ImageObject();
            imageObject.url = imageMatcher.group(1);
            raw.image = imageObject;
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
                    String quote = description.substring(quoteBegin, quoteEnd);

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

        raw.description = description.trim();
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
                GECkO.logger.debug("Before: {}", e1.getDescription());
                GECkO.logger.debug("After: {}", e2.description);
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
