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

package ch.ethz.geco.gecko.command.misc;

import ch.ethz.geco.gecko.GECkO;
import ch.ethz.geco.gecko.MediaSynchronizer;
import ch.ethz.geco.gecko.command.Command;
import ch.ethz.geco.gecko.command.CommandUtils;
import ch.ethz.geco.gecko.rest.api.GecoAPI;
import sun.misc.MessageUtils;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.MessageBuilder;
import sx.blah.discord.util.MessageHistory;

import java.util.List;

public class Test extends Command {
    public Test() {
        this.setName("test");
    }

    @Override
    public void execute(IMessage msg, List<String> args) {
        //MediaSynchronizer.loadNews();

        EmbedObject.VideoObject videoObject = new EmbedObject.VideoObject();
        videoObject.url = "https://www.youtube.com/watch?v=64xUcPqe2jM";

        EmbedObject test = new EmbedObject();
        test.type = "video";
        test.video = videoObject;

        MessageBuilder messageBuilder = new MessageBuilder(GECkO.discordClient);
        messageBuilder.withEmbed(test).withChannel(GECkO.mainChannel).build();
    }
}
