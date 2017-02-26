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

import ch.ethz.geco.gecko.command.core.*;
import ch.ethz.geco.gecko.command.misc.Whois;
import ch.ethz.geco.gecko.command.vote.CVote;

/**
 * This class is just for having all registered commands in one place
 */
public class CommandBank {
    /**
     * Registers all commands in the CommandRegistry
     */
    public static void registerCommands() {
        // Core
        CommandRegistry.registerCommand(new Ping());
        CommandRegistry.registerCommand(new Restart());
        CommandRegistry.registerCommand(new Update());
        CommandRegistry.registerCommand(new Upstart());

        // Misc
        CommandRegistry.registerCommand(new Whois());

        // Vote
        CommandRegistry.registerCommand(new CVote());
    }
}
