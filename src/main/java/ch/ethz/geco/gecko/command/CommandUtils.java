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
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;

public class CommandUtils {
    /**
     * Responds to the message which triggered this command.
     *
     * @param msg  the message which triggered this command
     * @param text the response text
     */
    public static void respond(IMessage msg, String text) {
        RequestBuffer.request(() -> {
            try {
                msg.getChannel().sendMessage(text);
            } catch (MissingPermissionsException e) {
                GECkO.logger.error("[CommandUtils] Missing permissions to respond to command message.");
                e.printStackTrace();
            } catch (DiscordException e) {
                GECkO.logger.error("[CommandUtils] Could not respond to command message: " + e.getErrorMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Deletes the message which triggered this command.
     *
     * @param msg the message which triggered this command
     */
    public static void deleteMessage(IMessage msg) {
        if (!msg.getChannel().isPrivate()) {
            RequestBuffer.request(() -> {
                try {
                    msg.delete();
                } catch (MissingPermissionsException e) {
                    GECkO.logger.error("[CommandUtils] Missing permissions to delete command message.");
                    e.printStackTrace();
                } catch (DiscordException e) {
                    GECkO.logger.error("[CommandUtils] Could not delete command message: " + e.getErrorMessage());
                    e.printStackTrace();
                }
            });
        }
    }
}
