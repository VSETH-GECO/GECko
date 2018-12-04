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

import ch.ethz.geco.g4j.impl.GECoClient;
import ch.ethz.geco.g4j.obj.IGECoClient;
import ch.ethz.geco.gecko.command.CommandBank;
import ch.ethz.geco.gecko.command.CommandHandler;
import ch.ethz.geco.gecko.command.CommandUtils;
import ch.ethz.geco.gecko.rest.WebHookServer;
import org.slf4j.LoggerFactory;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.DiscordException;

public class GECkO {
    /**
     * The Discord client used by the bot.
     */
    public static IDiscordClient discordClient;

    /**
     * The GECo client used by the bot.
     */
    public static IGECoClient gecoClient;

    /**
     * The main channel where the bot posts debug/testing stuff.
     */
    public static IChannel mainChannel;

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
            CommandHandler.setDefaultPrefix(ConfigManager.getProperties().getProperty("main_defaultPrefix"));
        }

        try {
            // Login with bot token
            ClientBuilder builder = new ClientBuilder();
            if (token != null) {
                discordClient = builder.withToken(token).login();
            } else {
                discordClient = builder.withToken(ConfigManager.getProperties().getProperty("main_token")).login();
            }

            // Register default event handler
            discordClient.getDispatcher().registerListener(new EventHandler());
        } catch (DiscordException e) {
            logger.error("Failed to login: " + e.getErrorMessage());
        }
    }

    /**
     * Called after the Discord API is ready to operate.
     */
    public static void postInit() {
        // Login
        gecoClient = new GECoClient(ConfigManager.getProperties().getProperty("geco_apiKey"));

        // Set main channel
        mainChannel = discordClient.getChannelByID(Long.valueOf(ConfigManager.getProperties().getProperty("main_mainChannelID")));

        // Register all commands
        CommandBank.registerCommands();

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(GECkO::preShutdown));

        // Start to listen to commands after initializing everything else
        discordClient.getDispatcher().registerListener(new CommandHandler());

        MediaSynchronizer.startPeriodicCheck();

        CommandUtils.respond(mainChannel, "**Initialized!**");
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
