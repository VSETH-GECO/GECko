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
 * For more information, please refer to <http://unlicense.org/>
 */

package ch.ethz.geco.gecko.command.core;

import ch.ethz.geco.gecko.GECkO;
import ch.ethz.geco.gecko.command.Command;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.Image;
import sx.blah.discord.util.RequestBuffer;

import java.util.List;

public class Avatar extends Command {
    public Avatar() {
        this.setName("avatar");
        this.setParams("<URL>");
        this.setDescription("Sets the avatar for the bot.");
        this.getPermissions().getPermittedRoleIDs().add("292488871055327232");
    }

    @Override
    public void execute(IMessage msg, List<String> args) {
        RequestBuffer.request(() -> {
            try {
                GECkO.discordClient.changeAvatar(Image.forUrl(null, args.get(0)));
            } catch (DiscordException e) {
                GECkO.logger.error("[Avatar] Could not change avatar: " + e.getErrorMessage());
                e.printStackTrace();
            }
        });
    }
}
