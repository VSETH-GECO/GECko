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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

/**
 * Manages all your config needs
 */
public class ConfigManager {
    /**
     * Stores all core fields. The core fields are the minimum needed fields to run the bot.
     */
    private static final Map<String, String> coreFields = new LinkedHashMap<>();

    /**
     * The file path of the default config file.
     */
    private static final String defaultPath = "data/bot.properties";

    /**
     * The file path of the current confing file.
     */
    private static String currentPath = defaultPath;

    /**
     * The bot properties.
     */
    private static Properties properties;

    /**
     * Returns the properties.
     *
     * @return the properties
     */
    public static Properties getProperties() {
        return properties;
    }

    /**
     * Sets the path of the config to load. You most likely have to reload the config using loadConfig() after changing the config path.
     *
     * @param configPath the path of the new config file
     */
    public static void setConfigPath(String configPath) {
        ConfigManager.currentPath = configPath;
    }

    /**
     * Loads the configuration file or creates a new one if it doesn't exist.
     */
    public static void loadConfig() {
        try {
            File file = new File(currentPath);
            if (!file.isFile()) {
                if (!file.createNewFile()) {
                    throw new IOException("Could not create new file: " + currentPath);
                }
            }

            properties = new Properties();

            FileInputStream inputStream = new FileInputStream(currentPath);
            properties.load(inputStream);
            inputStream.close();
        } catch (IOException e) {
            ErrorHandler.handleError(e);
        }
    }

    /**
     * Saves the configuration file.
     */
    public static void saveConfig() {
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(currentPath);
            properties.store(outputStream, "");
            outputStream.close();
        } catch (IOException e) {
            ErrorHandler.handleError(e);
        }
    }

    /**
     * Adds a core field to check on startup.
     *
     * @param field          the field name
     * @param defaultMessage the default message the user gets when the field is missing followed by an input
     */
    private static void addCoreField(String field, String defaultMessage) {
        coreFields.put(field, defaultMessage);
    }

    /**
     * Registers all the core fields needed to run the bot.
     */
    public static void addCoreFields() {
        addCoreField("main_token", "[Main] Please specify the bot token: ");
        addCoreField("main_mainChannelID", "[Main] Please specify the main channel ID: ");
        addCoreField("main_defaultPrefix", "[Main] Please specify a default command prefix: ");

        addCoreField("media_newsChannelID", "[Media] Please specify the news channel ID: ");
        addCoreField("media_eventChannelID", "[Media] Please specify the event channel ID: ");

        addCoreField("git_botRepo", "[Git] Please specify the SSH remote repository of the bot: ");
        addCoreField("git_privateKeyFile", "[Git] Please specify the private key file: ");
        addCoreField("git_knownHostsFile", "[Git] Please specify known hosts file: ");

        addCoreField("geco_apiKey", "[GECO] Please specify the GECO API Key: ");
        addCoreField("pastebin_apiKey", "[Pastebin] Please specify the Pastebin API key: ");
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static Object setProperty(String key, String value) {
        return properties.setProperty(key, value);
    }

    /**
     * Checks for the core configurations and asks the user to input them if missing.
     */
    public static void checkCoreFields() {
        for (String key : coreFields.keySet()) {
            if (!properties.containsKey(key)) {
                GECko.logger.info(coreFields.get(key));
                Scanner scanner = new Scanner(System.in);

                properties.setProperty(key, scanner.nextLine());
            }
        }

        saveConfig();
    }
}