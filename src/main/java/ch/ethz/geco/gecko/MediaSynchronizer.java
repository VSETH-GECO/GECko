package ch.ethz.geco.gecko;

import ch.ethz.geco.gecko.rest.RequestBuilder;
import ch.ethz.geco.gecko.rest.api.exception.APIException;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Synchronizes the news and events from the website with the corresponding channels. <p>
 * See {@link ch.ethz.geco.gecko.rest.WebHookServer} for details.
 */
public class MediaSynchronizer {
    private static final IChannel newsChannel = GECkO.mainChannel; //GECkO.discordClient.getChannelByID(366931520667123712L);
    private static final IChannel eventChannel = GECkO.mainChannel; //GECkO.discordClient.getChannelByID(366931534579630081L);

    private static final LinkedHashMap<Integer, IMessage> news = new LinkedHashMap<>();
    private static final LinkedHashMap<Integer, IMessage> events = new LinkedHashMap<>();

    private static final Pattern idPattern = Pattern.compile("/(\\d+)/?");

    /**
     * Loads the news and events from the website.
     */
    public static void loadMedia() {
        try {
            HttpResponse response = new RequestBuilder("https://geco.ethz.ch/api/v2/web/news")
                    .addHeader("Authorization", "Token token=" + ConfigManager.getProperties().getProperty("geco_apiKey"))
                    .get();

            StatusLine statusLine = response.getStatusLine();
            //Header encoding = response.getEntity().getContentEncoding();
            //String content = IOUtils.toString(response.getEntity().getContent(), encoding.getValue() != null ? encoding.getValue() : "UTF-8");

            switch (statusLine.getStatusCode()) {
                case 200:
                    List<EmbedObject> embeds = DiscordUtils.MAPPER.readValue(response.getEntity().getContent(), DiscordUtils.MAPPER.getTypeFactory().constructCollectionType(List.class, EmbedObject.class));
                    MessageHistory newsHistory = newsChannel.getMessageHistory(10);

                    for (EmbedObject embed : embeds) {
                        Matcher embedIDMatcher = idPattern.matcher(embed.url);
                        if (embedIDMatcher.find()) {
                            int embedID = Integer.parseInt(embedIDMatcher.group(1));

                            for (IMessage message : newsHistory) {
                                IEmbed messageEmbed = message.getEmbeds().get(0);
                                if (messageEmbed == null) {
                                    message.delete();
                                    continue;
                                }

                                Matcher messIDMatcher = idPattern.matcher(messageEmbed.getUrl());


                            }
                        } else {
                            ErrorHandler.handleError(new APIException("Received news post without ID. Contact the Web Master!"));
                        }
                    }

                    int lastID = -1;
                    for (IMessage newsPost : newsHistory) {
                        IEmbed embed = newsPost.getEmbeds().get(0);
                        if (embed != null) {
                            String url = embed.getUrl();
                            Matcher idMatcher = idPattern.matcher(url);
                            if (idMatcher.find()) {
                                int newsID = Integer.valueOf(idMatcher.group(1));
                                if (newsID > lastID) {
                                    setNews(newsID, embedToObject(embed));
                                } else {

                                }
                            } else {
                                GECkO.logger.warn("[MediaSynchronizer] Loaded message with invalid news id, deleting...");
                                newsPost.delete();
                            }
                        } else {
                            GECkO.logger.warn("[MediaSynchronizer] Loaded message with invalid embed, deleting...");
                            newsPost.delete();
                        }
                    }

                    break;
                case 400:
                    ErrorHandler.handleError(new IOException("Bad Request"));
                    break;
                case 401:
                    ErrorHandler.handleError(new IOException("Bot is unauthorized."));
                    break;
                case 403:
                    ErrorHandler.handleError(new IOException("Forbidden"));
                    break;
                case 404:
                    ErrorHandler.handleError(new IOException("Not found"));
                    break;
                case 500:
                    ErrorHandler.handleError(new IOException("Internal Server Error"));
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
        setEvent(id, message, true);
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
            message = processImages(message);
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
     * Removes all image tags from the description and adds the first image as the main image.
     *
     * @param raw the embed with unprocessed description
     * @return the embed with image tags removed and first image added as main image.
     */
    private static EmbedObject processImages(EmbedObject raw) {
        // Get first image
        Pattern imageURLPattern = Pattern.compile("!\\[[^]]*]\\(([^)]*)\\)");
        Matcher matcher = imageURLPattern.matcher(raw.description);
        if (matcher.find()) {
            String imageURL = matcher.group(1);

            EmbedObject.ImageObject imageObject = new EmbedObject.ImageObject();
            imageObject.url = imageURL;
            raw.image = imageObject;

            // Remove all image tags
            raw.description = raw.description.replaceAll("!\\[[^]]*]\\([^)]*\\)", "");
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

    private static boolean isEqual(IEmbed e1, IEmbed e2) {
        return e1.getAuthor().getIconUrl().equals(e2.getAuthor().getIconUrl()) &&
                e1.getAuthor().getName().equals(e2.getAuthor().getName()) &&
                e1.getAuthor().getUrl().equals(e2.getAuthor().getUrl()) &&
                e1.getTitle().equals(e2.getTitle()) &&
                e1.getUrl().equals(e2.getUrl()) &&
                e1.getDescription().equals(e2.getDescription()) &&
                e1.getFooter().getText().equals(e2.getFooter().getText());
    }
}
