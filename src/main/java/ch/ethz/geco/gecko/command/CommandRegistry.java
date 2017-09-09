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

package ch.ethz.geco.gecko.command;

import ch.ethz.geco.gecko.GECkO;

import java.util.HashMap;
import java.util.Map;

public class CommandRegistry {
    /**
     * Stores a mapping of all registered prefixes and another map with a mapping of all command names with the command.
     */
    private static Map<String, Map<String, Command>> prefixCommands = new HashMap<>();

    /**
     * Stores a mapping of all mention commands with the corresponding commands.
     */
    private static Map<String, Command> mentionCommands = new HashMap<>();

    /**
     * Used to register a new command. After registering, a command will be triggered if a matching message arrives.
     *
     * @param cmd the command to register
     */
    public static void registerCommand(Command cmd) {
        // If it's a mention command
        if (cmd.isMentionCommand()) {
            for (String alias : cmd.getNames()) {
                if (!mentionCommands.containsKey(alias)) {
                    mentionCommands.put(alias, cmd);
                } else {
                    GECkO.logger.error("[CommandRegistry] Mention alias <" + alias + "> already defined in class: " + mentionCommands.get(alias).getClass().getSimpleName());
                }
            }
        } else {    // Otherwise it's a prefix command
            String prefix = cmd.getPrefix();

            // Use default prefix if prefix not defined
            if (prefix == null) {
                prefix = CommandHandler.getDefaultPrefix();
            }

            // Add new HashMap if not existing
            if (!prefixCommands.containsKey(prefix)) {
                prefixCommands.put(prefix, new HashMap<>());
            }

            // Put command into matching prefix map
            for (String alias : cmd.getNames()) {
                if (!prefixCommands.get(prefix).containsKey(alias)) {
                    prefixCommands.get(prefix).put(alias, cmd);
                } else {
                    GECkO.logger.error("[CommandRegistry] Prefix alias <" + alias + "> already defined in class: " + prefixCommands.get(prefix).get(alias).getClass().getSimpleName());
                }
            }
        }
    }

    /**
     * Used to find and get a command from the registry.
     *
     * @param name the name of the command including the prefix
     * @return the command or null if not existing
     */
    public static Command getPrefixCommand(String name) {
        for (String prefix : prefixCommands.keySet()) {
            // If there is a command matching the prefix
            if (name.startsWith(prefix)) {
                String cmdName = name.substring(prefix.length());   // Remove prefix from name
                return prefixCommands.get(prefix).get(cmdName);
            }
        }

        return null;
    }

    /**
     * Returns all prefix commands with the following mapping: {@code Map<Prefix, Map<Alias, Command Object>>}
     *
     * @return all prefix commands
     */
    public static Map<String, Map<String, Command>> getPrefixCommands() {
        return prefixCommands;
    }

    /**
     * Used to find and get a mention command from the registry.
     *
     * @param name the name of the command
     * @return the command or null if not existing
     */
    public static Command getMentionCommand(String name) {
        return mentionCommands.get(name);
    }

    /**
     * Reurns all mention commands with the following mapping: {@code Map<Alias, Command Object>}
     *
     * @return all mention commands
     */
    public static Map<String, Command> getMentionCommands() {
        return mentionCommands;
    }
}
