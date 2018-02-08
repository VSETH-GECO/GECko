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
import com.fasterxml.jackson.databind.deser.DataFormatReaders;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import sx.blah.discord.api.internal.DiscordUtils;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IEmbed;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.MessageHistory;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Synchronizes the news and events from the website with the corresponding channels. <p>
 * See {@link ch.ethz.geco.gecko.rest.WebHookServer} for details.
 */
public class MediaSynchronizer {
    private static final IChannel newsChannel = GECkO.discordClient.getChannelByID(Long.valueOf(ConfigManager.getProperties().getProperty("media_newsChannelID")));
    private static final IChannel eventChannel = GECkO.discordClient.getChannelByID(Long.valueOf(ConfigManager.getProperties().getProperty("media_eventChannelID")));

    private static final LinkedHashMap<Integer, IMessage> news = new LinkedHashMap<>();
    private static final LinkedHashMap<Integer, IMessage> events = new LinkedHashMap<>();

    private static final Pattern idPattern = Pattern.compile("/(\\d+)/?");

    /**
     * Loads the news from the website. This should only be used once every restart.
     * It will load last page of news and check for every existing news entry if it's up-to-date.
     */
    public static void loadNews() {
        try {
            HttpResponse response = new RequestBuilder("https://geco.ethz.ch/api/v2/web/news")
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

                    MessageHistory newsHistory = newsChannel.getMessageHistory(10);
                    DualHashBidiMap<Integer, IMessage> newsPostMap = new DualHashBidiMap<>();

                    // Sort embeds by news ID or remove them if they don't have a news ID
                    webEmbeds.sort(Comparator.comparing(embedObject -> {
                        Matcher idMatcher = idPattern.matcher(embedObject.url);

                        if (idMatcher.find()) {
                            int id = Integer.parseInt(idMatcher.group(1));
                            webEmbedMap.put(id, embedObject);
                            return id;
                        } else {
                            // Remove embed if it has an invalid ID
                            webEmbeds.remove(embedObject);
                            GECkO.logger.warn("Received news post without ID. Contact the Web Master!");
                            return 0;
                        }
                    }));

                    // Sort and validate news history
                    newsHistory.sort(Comparator.comparing(message -> {
                        IEmbed embed = message.getEmbeds().get(0);

                        if (embed != null) {
                            Matcher idMatcher = idPattern.matcher(embed.getUrl());

                            if (idMatcher.find()) {
                                int id = Integer.parseInt(idMatcher.group(1));
                                newsPostMap.put(id, message);
                                return id;
                            }
                        }

                        // Remove message if something fails, we will repost missing messages later on.
                        newsHistory.remove(message);
                        message.delete();
                        GECkO.logger.warn("Found invalid message in news channel!");
                        return 0;
                    }));

                    // Remove old news
                    for (IMessage newsPost : newsHistory) {
                        Integer newsID = newsPostMap.getKey(newsPost);

                        if (!webEmbedMap.containsKey(newsID)) {
                            newsHistory.remove(newsPost);
                            newsPostMap.remove(newsID, newsPost);
                            newsPost.delete();
                        }
                    }

                    // At this point, all messages which were not in the API response should have been removed
                    // and all remaining messages should have been sorted ascending by their web ID (not Discord ID!)

                    // We will then iterate through the message list and the embed list we got from the REST API at the
                    // same time. If an embed id matches a message id, we will verify the content of the message and modify
                    // it, if there were changes. If the embed id is not equal to the message id, we will override the
                    // current message with the current embed. When were out of messages but there are still embeds left,
                    // we will simply post new messages. Example:

                    /*
                     * Scenario:
                     * Embed ID (Web) | Message ID    | Operation | Result ID
                     *
                     * 8              | 8             | 8  (check and edit)
                     * 9              | 12            | 9  (override)
                     * 10             | x             | 10 (post)
                     * 11             | x             | 11 (post)
                     * 12             | x             | 12 (post)
                     *
                     */

                    newsChannel.sendMessage(processImages(extendAuthorIconUrl(webEmbeds.get(8))));

                    /*
                    for (int i = 0; i < webEmbeds.size(); i++) {
                        EmbedObject webEmbed = webEmbeds.get(i);
                        int webID = webEmbedMap.getKey(webEmbed);

                        // If we have more embeds than messages in the news history, post new messages
                        if (i >= newsHistory.size()) {
                            setNews(webID, webEmbed);
                        } else { // Otherwise, either replace or validate the news history
                            IMessage localNews = newsHistory.get(i);
                            int localID = newsPostMap.getKey(localNews);

                            // If local and web embed have the same news ID, validate and update local content
                            if (webID == localID) {
                                if (!embedEqualsEmbedObject(localNews.getEmbeds().get(0), webEmbed)) {
                                    news.put(webID, localNews.edit(webEmbed));
                                } else {
                                    GECkO.logger.debug("Local news post " + webID + " is up-to-date.");
                                }
                            } else { // Otherwise override local embed with web embed
                                news.put(webID, localNews.edit(webEmbed));
                            }
                        }
                    }*/

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
            message = processImages(message);
        }

        if (news.containsKey(id)) {
            news.get(id).edit(message);
        } else {
            news.put(id, newsChannel.sendMessage(message));
        }
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
            message = processImages(extendAuthorIconUrl(message));
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
        raw.author.icon_url = "https://geco.ethz.ch" + raw.author.icon_url;
        return raw;
    }

    /**
     * Removes all image tags from the description and adds the first image as the main image.
     *
     * @param raw the embed with unprocessed description
     * @return the embed with image tags removed and first image added as main image.
     */
    private static EmbedObject processImages(EmbedObject raw) {
        Pattern youtubePattern = Pattern.compile("http(?:s?)://(?:www\\.)?youtu(?:be\\.com/watch\\?v=|\\.be/)[\\w\\-_]*(&(amp;)?\u200C\u200B[\\w?\u200C\u200B=]*)?");
        Pattern imagePattern = Pattern.compile("!\\[[^]]*]\\(([^)]*)\\)");
        Pattern nestedImagePattern = Pattern.compile("\\[!\\[([^]]*)]\\([^)]*\\)]\\(([^)]*)\\)");
        Matcher imageMatcher = imagePattern.matcher(raw.description);

        // Find first image and use it as message image, remove all remaining images.
        if (imageMatcher.find()) {
            Matcher nestedMatcher = nestedImagePattern.matcher(raw.description);
            if (nestedMatcher.find()) {
                Matcher youtubeMatcher = youtubePattern.matcher(nestedMatcher.group(2));
                nestedMatcher.group(1);

                //nestedMatcher.replaceFirst()
            } else {
                String imageURL = imageMatcher.group(1);

                EmbedObject.ImageObject imageObject = new EmbedObject.ImageObject();
                imageObject.url = imageURL;
                raw.image = imageObject;

                // Remove all image tags
                raw.description = raw.description.replaceAll("!\\[[^]]*]\\([^)]*\\)", "");
            }
        }

        return raw;
    }

    /**
     * Converts an IEmbed to an EmbedObject. But only converts fields relevant to news or event posts.
     *
     * @param embed the IEmbed to convert
     * @return the corresponding EmbedObject
     */
    private static EmbedObject embedToObject(IEmbed embed) {
        EmbedBuilder embedBuilder = new EmbedBuilder().withTitle(embed.getTitle())
                .withDescription(embed.getDescription())
                .withUrl(embed.getUrl());

        if (embed.getAuthor() != null) {
            embedBuilder.withAuthorName(embed.getAuthor().getName())
                    .withAuthorIcon(embed.getAuthor().getIconUrl())
                    .withAuthorUrl(embed.getAuthor().getUrl());
        }

        if (embed.getFooter() != null) {
            embedBuilder.withFooterText(embed.getFooter().getText());
        }

        return embedBuilder.build();
    }

    /**
     * Returns whether or not the given embeds are the same in terms of a news or event post.
     *
     * @param e1 the first embed
     * @param e2 the second embed
     * @return if the two embeds are equal
     */
    private static boolean embedEqualsEmbedObject(IEmbed e1, EmbedObject e2) {
        return e1.getAuthor().getIconUrl().equals(e2.author.icon_url) &&
                e1.getAuthor().getName().equals(e2.author.name) &&
                e1.getAuthor().getUrl().equals(e2.author.url) &&
                e1.getTitle().equals(e2.title) &&
                e1.getUrl().equals(e2.url) &&
                e1.getDescription().equals(e2.description) &&
                e1.getFooter().getText().equals(e2.footer.text);
    }
}
