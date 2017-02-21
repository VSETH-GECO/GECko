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

package ch.ethz.geco.gecko.command.core;

import ch.ethz.geco.gecko.GECkO;
import ch.ethz.geco.gecko.command.Command;
import ch.ethz.geco.gecko.command.CommandUtils;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.RequestBuffer;

import java.util.List;
import java.util.Objects;

public class Restart extends Command {
    public Restart() {
        this.setName("restart");
        this.setDescription("Restarts the bot.");
    }

    @Override
    public void execute(IMessage msg, List<String> args) {
        GECkO.logger.debug("[RESTART] Getting connected voice channels...");
        List<IVoiceChannel> connectedVoiceChannels = GECkO.discordClient.getConnectedVoiceChannels();
        GECkO.logger.debug("[RESTART] Leaving all connected voice channels...");
        connectedVoiceChannels.stream().filter(voiceChannel -> Objects.equals(voiceChannel.getGuild().getID(), msg.getGuild().getID())).forEach(IVoiceChannel::leave);
        CommandUtils.respond(msg, "[INFO] Restarting bot...");
        CommandUtils.deleteMessage(msg);

        GECkO.logger.debug("[RESTART] Trying to shutdown bot:");
        RequestBuffer.request(() -> { try {
            GECkO.logger.debug("[RESTART] - Logging out...");
            GECkO.discordClient.logout();
            GECkO.logger.debug("[RESTART] - calling System.exit(0)...");
            new Thread(() -> System.exit(0)).start();
        } catch (DiscordException e) {
            GECkO.logger.debug("[RESTART] An error occured during restart.");
            e.printStackTrace();
        }});
    }
}
