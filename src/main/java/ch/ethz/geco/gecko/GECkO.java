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
 * For more information, please refer to <http://unlicense.org>
 */

package ch.ethz.geco.gecko;

import ch.ethz.geco.g4j.impl.DefaultGECoClient;
import ch.ethz.geco.g4j.obj.GECoClient;
import ch.ethz.geco.gecko.command.CommandBank;
import ch.ethz.geco.gecko.command.CommandHandler;
import ch.ethz.geco.gecko.command.CommandUtils;
import ch.ethz.geco.gecko.rest.WebHookServer;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Snowflake;
import org.slf4j.LoggerFactory;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.DiscordException;

import javax.xml.soap.Text;

public class GECkO {
    /**
     * The Discord client used by the bot.
     */
    public static DiscordClient discordClient;

    /**
     * The GECo client used by the bot.
     */
    public static GECoClient gecoClient;

    /**
     * The main channel where the bot posts debug/testing stuff.
     */
    public static TextChannel mainChannel;

    /**
     * The main guild where the bot is.
     */
    public static Guild mainGuild;

    /**
     * If the bot was initialized once.
     */
    private static boolean initOnce = false;

    /**
     * The main logger of the bot.
     */
    public static final org.slf4j.Logger logger = LoggerFactory.getLogger(GECkO.class);

    public static void main(String[] args) {
        String token = null;
        String prefix = null;
        String configPath = null;

        // Parse options
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--token":
                    if (i + 1 < args.length) {
                        token = args[i + 1];
                    }
                    break;
                case "--prefix":
                    if (i + 1 < args.length) {
                        prefix = args[i + 1];
                    }
                    break;
                case "--config":
                    if (i + 1 < args.length) {
                        configPath = args[i + 1];
                    }
                    break;
                default:
                    break;
            }
        }

        GECkO.start(token, prefix, configPath);
        new WebHookServer(8080);
    }

    public static void start(String token, String prefix, String configPath) {
        logger.info("GECkO");
        logger.info("The official GECO Discord bot. 2016 - 2018, Licensed under Unlicense.");

        // Initialize config manager
        if (configPath != null) {
            ConfigManager.setConfigPath(configPath);
        }

        ConfigManager.loadConfig();
        ConfigManager.addCoreFields();
        ConfigManager.checkCoreFields();

        // Set command prefix
        if (prefix != null) {
            CommandHandler.setDefaultPrefix(prefix);
        } else {
            CommandHandler.setDefaultPrefix(ConfigManager.getProperty("main_defaultPrefix"));
        }

        DiscordClientBuilder builder = new DiscordClientBuilder(ConfigManager.getProperty("main_token"));
        discordClient = builder.build();

        EventDispatcher eventDispatcher = discordClient.getEventDispatcher();

        eventDispatcher.on(ReadyEvent.class).subscribe(readyEvent -> {
            mainChannel = discordClient.getChannelById(Snowflake.of(ConfigManager.getProperties().getProperty("main_mainChannelID")))
                    .ofType(TextChannel.class).block();

            if (mainChannel != null) {
                mainGuild = mainChannel.getGuild().block();

                postInit();
            }
        });

    }

    /**
     * Called after the Discord API is ready to operate.
     */
    public static void postInit() {
        // Listen to messages
        discordClient.getEventDispatcher().on(MessageCreateEvent.class).subscribe(CommandHandler::handle);

        // Stuff you only want to be initialized once
        if (!initOnce) {
            // Login
            gecoClient = new DefaultGECoClient(ConfigManager.getProperties().getProperty("geco_apiKey"));

            // Register all commands
            CommandBank.registerCommands();

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(GECkO::preShutdown));

            // Start to listen to commands after initializing everything else
            discordClient.getDispatcher().registerListener(new CommandHandler());

            // Start periodic news and event updates
            MediaSynchronizer.startPeriodicCheck();
        }

        if (initOnce) {
            CommandUtils.respond(mainChannel, "**Reconnected!**");
        } else {
            CommandUtils.respond(mainChannel, "**Initialized!**");
        }

        initOnce = true;
    }

    /**
     * Called before shutting down.
     * Can be used to clean up stuff and/or to finish work.
     */
    private static void preShutdown() {
        logger.info("Shutting down...");

        ConfigManager.saveConfig();
    }
}
