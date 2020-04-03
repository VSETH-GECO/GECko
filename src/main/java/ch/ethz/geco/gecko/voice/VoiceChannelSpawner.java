package ch.ethz.geco.gecko.voice;

import ch.ethz.geco.gecko.ConfigManager;
import ch.ethz.geco.gecko.command.CommandRegistry;
import ch.ethz.geco.gecko.voice.command.VCSpawner;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Category;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.VoiceChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.util.Snowflake;

import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static ch.ethz.geco.gecko.GECko.discordClient;

public class VoiceChannelSpawner {
    // Emojis used by the voice channel spawner
    public static final String EMOJI_INF = "♾️";
    public static final String EMOJI_TWO = "2️⃣";
    public static final String EMOJI_THREE = "3️⃣";
    public static final String EMOJI_FOUR = "4️⃣";
    public static final String EMOJI_FIVE = "5️⃣";
    public static final String EMOJI_SIX = "6️⃣";
    public static final String EMOJI_SEVEN = "7️⃣";
    public static final String EMOJI_EIGHT = "8️⃣";
    public static final String EMOJI_NINE = "9️⃣";
    public static final String EMOJI_TEN = "\uD83D\uDD1F";

    /**
     * The scheduler used for delayed voice channel deletion.
     */
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /**
     * Maps message IDs to category IDs.
     */
    private static final Map<Snowflake, Snowflake> voiceChannelSpawner = new HashMap<>();

    /**
     * A list of all spawned voice channels.
     */
    private static final List<Snowflake> spawnedChannels = new ArrayList<>();

    /**
     * Initializes the voice channel spawner, loading configurations and cleaning up.
     * This should be called once on startup.
     */
    public static void init() {
        // Voice
        CommandRegistry.registerCommand(new VCSpawner());

        if (ConfigManager.getProperty("vcspawner_list") != null) {
            String vcspawnerList = ConfigManager.getProperty("vcspawner_list");

            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(Base64.getDecoder().decode(vcspawnerList));
                ObjectInputStream ois = new ObjectInputStream(bis);
                Map<String, String> voiceChannelSpawnerStrings = (Map<String, String>) ois.readObject();
                ois.close();
                bis.close();

                voiceChannelSpawnerStrings.forEach((k, v) -> voiceChannelSpawner.put(Snowflake.of(k), Snowflake.of(v)));

                // TODO: Figure out a way to reset reactions on all spawned channels on init
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        // Register voice channel spawner events
        discordClient.getEventDispatcher().on(ReactionAddEvent.class).subscribe(VoiceChannelSpawner::handleReaction);
        discordClient.getEventDispatcher().on(VoiceStateUpdateEvent.class).subscribe(VoiceChannelSpawner::handleVoiceUpdate);
    }

    /**
     * Creates a new voice channel spawner in the given text channel.
     *
     * @param textChannel The text channel where the voice channel spawner should be created.
     */
    public static void createSpawner(TextChannel textChannel) {
        textChannel.createMessage("**Voice Channel Spawner**\n" +
                "React to this message to create a temporary voice channel. The channel will be removed if no one joins after 10 seconds or if everyone leaves.\n" +
                "\n" +
                EMOJI_INF + " : Spawns an unlimited voice channel\n" +
                "\n" +
                "Spawn limited voice channels: \n" +
                EMOJI_TWO + " : Limited to 2 users\n" +
                EMOJI_THREE + " : Limited to 3 users\n" +
                EMOJI_FOUR + " : Limited to 4 users\n" +
                EMOJI_FIVE + " : Limited to 5 users\n" +
                EMOJI_SIX + " : Limited to 6 users\n" +
                EMOJI_SEVEN + " : Limited to 7 users\n" +
                EMOJI_EIGHT + " : Limited to 8 users\n" +
                EMOJI_NINE + " : Limited to 9 users\n" +
                EMOJI_TEN + " : Limited to 10 users\n")
                .subscribe(message -> {
                    // Add initial reactions
                    message.addReaction(ReactionEmoji.unicode(EMOJI_INF)).block();
                    message.addReaction(ReactionEmoji.unicode(EMOJI_TWO)).block();
                    message.addReaction(ReactionEmoji.unicode(EMOJI_THREE)).block();
                    message.addReaction(ReactionEmoji.unicode(EMOJI_FOUR)).block();
                    message.addReaction(ReactionEmoji.unicode(EMOJI_FIVE)).block();
                    message.addReaction(ReactionEmoji.unicode(EMOJI_SIX)).block();
                    message.addReaction(ReactionEmoji.unicode(EMOJI_SEVEN)).block();
                    message.addReaction(ReactionEmoji.unicode(EMOJI_EIGHT)).block();
                    message.addReaction(ReactionEmoji.unicode(EMOJI_NINE)).block();
                    message.addReaction(ReactionEmoji.unicode(EMOJI_TEN)).block();

                    voiceChannelSpawner.put(message.getId(), textChannel.getCategoryId().orElse(Snowflake.of(0)));

                    saveConfig();
                });
    }

    private static void saveConfig() {
        // Save config
        Map<String, String> serializedSpawnerMap = new HashMap<>();
        voiceChannelSpawner.forEach((k, v) -> serializedSpawnerMap.put(k.asString(), v.asString()));

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(serializedSpawnerMap);
            oos.close();
            bos.close();

            ConfigManager.setProperty("vcspawner_list", Base64.getEncoder().encodeToString(bos.toByteArray()));
            ConfigManager.saveConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void removeSpawner(Snowflake message) {
        voiceChannelSpawner.remove(message);
        saveConfig();
    }

    public static void clearSpawners() {
        voiceChannelSpawner.clear();
        saveConfig();
    }

    public static void handleReaction(ReactionAddEvent event) {
        if (discordClient.getSelfId().isPresent() && !event.getUserId().equals(discordClient.getSelfId().get()) && voiceChannelSpawner.containsKey(event.getMessageId())) {
            if (event.getEmoji().asUnicodeEmoji().isPresent()) {
                ReactionEmoji.Unicode unicode = event.getEmoji().asUnicodeEmoji().get();

                int userLimit = -1;
                switch (unicode.getRaw()) {
                    case EMOJI_INF:
                        userLimit = 0;
                        break;
                    case EMOJI_TWO:
                        userLimit = 2;
                        break;
                    case EMOJI_THREE:
                        userLimit = 3;
                        break;
                    case EMOJI_FOUR:
                        userLimit = 4;
                        break;
                    case EMOJI_FIVE:
                        userLimit = 5;
                        break;
                    case EMOJI_SIX:
                        userLimit = 6;
                        break;
                    case EMOJI_SEVEN:
                        userLimit = 7;
                        break;
                    case EMOJI_EIGHT:
                        userLimit = 8;
                        break;
                    case EMOJI_NINE:
                        userLimit = 9;
                        break;
                    case EMOJI_TEN:
                        userLimit = 10;
                        break;
                    default:
                        break;
                }

                if (userLimit != -1) {
                    final int finalUserLimit = userLimit;
                    Snowflake categoryID = voiceChannelSpawner.get(event.getMessageId());
                    discordClient.getChannelById(categoryID)
                            .cast(Category.class)
                            .flatMap(category -> category.getGuild())
                            .flatMap(guild -> guild.createVoiceChannel(spec -> {
                                spec.setParentId(categoryID);
                                spec.setName("Temporary Channel");
                                if (finalUserLimit != 0) {
                                    spec.setUserLimit(finalUserLimit);
                                }
                            })).subscribe(voiceChannel -> {
                        spawnedChannels.add(voiceChannel.getId());
                        scheduler.schedule(() -> {
                            // Check if channel is still existing
                            if (spawnedChannels.contains(voiceChannel.getId())) {
                                voiceChannel.getVoiceStates().count().subscribe(userCount -> {
                                    if (userCount == 0) {
                                        spawnedChannels.remove(voiceChannel.getId());
                                        voiceChannel.delete().subscribe();
                                    }
                                });
                            }
                        }, 10, TimeUnit.SECONDS);
                    });
                }
            }

            event.getMessage().flatMap(message -> message.removeReaction(event.getEmoji(), event.getUserId())).subscribe();
        }
    }

    public static void handleVoiceUpdate(VoiceStateUpdateEvent event) {
        Snowflake channelID;
        if (event.getCurrent().getChannelId().isPresent()) {
            channelID = event.getCurrent().getChannelId().get();
        } else if (event.getOld().isPresent() && event.getOld().get().getChannelId().isPresent()) {
            channelID = event.getOld().get().getChannelId().get();
        } else {
            return;
        }

        // Only if it's a temp channel
        if (spawnedChannels.contains(channelID)) {
            discordClient.getChannelById(channelID).cast(VoiceChannel.class).subscribe(voiceChannel -> {
                if (spawnedChannels.contains(voiceChannel.getId())) {
                    voiceChannel.getVoiceStates().count().subscribe(userCount -> {
                        if (userCount == 0) {
                            spawnedChannels.remove(voiceChannel.getId());
                            voiceChannel.delete().subscribe();
                        }
                    });
                }
            });
        }
    }
}
