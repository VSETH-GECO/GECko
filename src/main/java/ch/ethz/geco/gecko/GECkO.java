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

import ch.ethz.geco.gecko.command.CommandBank;
import ch.ethz.geco.gecko.command.CommandHandler;
import ch.ethz.geco.gecko.command.CommandUtils;
import org.slf4j.LoggerFactory;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.DiscordException;

import java.io.IOException;

public class GECkO {
    /**
     * The discord client used by the bot.
     */
    public static IDiscordClient discordClient;

    /**
     * The main channel where the bot posts debug/testing stuff.
     */
    public static IChannel mainChannel;

    /**
     * The main logger of the bot.
     */
    public static final org.slf4j.Logger logger = LoggerFactory.getLogger(GECkO.class);

    /**
     * The command handler of the bot.
     */
    public static final CommandHandler commandHandler = new CommandHandler();

    public static void main(String[] args) {
        // Init message
        logger.info("GECkO v0.0.1");
        logger.info("The official GECO Discord bot.");

        // Load config files
        try {
            ConfigManager.loadConfig();
            ConfigManager.addCoreFields();
            ConfigManager.checkCoreFields();
            ConfigManager.saveConfig();
        } catch (IOException e) {
            logger.error("Could not load or create config files: " + e.getMessage());
            e.printStackTrace();
            logger.info("Shutting down...");
            System.exit(0);
        }

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(GECkO::preShutdown));

        ClientBuilder builder = new ClientBuilder();
        if (args.length >= 2 && args[0].equals("-p")) {  // User wants to set a custom prefix
            CommandHandler.setDefaultPrefix(args[1]);

            try {
                discordClient = builder.withToken(ConfigManager.getProperties().getProperty("token")).login();
            } catch (DiscordException e) {
                logger.error("Failed to login: " + e.getErrorMessage());
            }
        } else {    // User uses default prefix
            CommandHandler.setDefaultPrefix(ConfigManager.getProperties().getProperty("defaultPrefix"));

            try {
                discordClient = builder.withToken(ConfigManager.getProperties().getProperty("token")).login();
            } catch (DiscordException e) {
                logger.error("Failed to login: " + e.getErrorMessage());
            }
        }

        // Register default event handler
        EventDispatcher dispatcher = discordClient.getDispatcher();
        dispatcher.registerListener(new EventHandler());
    }

    /**
     * Called after the Discord API is ready to operate.
     */
    public static void postInit() {
        // Set main channel
        mainChannel = discordClient.getChannelByID(ConfigManager.getProperties().getProperty("mainChannelID"));

        // Register all commands
        CommandBank.registerCommands();

        // Start to listen to commands after initializing everything else
        EventDispatcher dispatcher = discordClient.getDispatcher();
        dispatcher.registerListener(new CommandHandler());

        CommandUtils.respond(mainChannel, "**Initialized!**");
    }

    /**
     * Called before shutting down.
     * Can be used to clean up stuff and/or to finish work.
     */
    private static void preShutdown() {
        logger.info("Shutting down...");

        try {
            ConfigManager.saveConfig();
        } catch (IOException e) {
            logger.error("Failed to save config file: " + e.getMessage());
        }
    }
}
