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

import ch.ethz.geco.gecko.MediaSynchronizer;
import ch.ethz.geco.gecko.command.Command;
import sx.blah.discord.handle.obj.IMessage;

import java.util.List;

public class Test extends Command {
    public Test() {
        this.setName("test");
    }

    @Override
    public void execute(IMessage msg, List<String> args) {
        /*String testString = "**bold**\n" +
                "*italic*\n" +
                "~~strike~~\n" +
                "# H1\n" +
                "## H2\n" +
                "### H3\n" +
                "#### H4\n" +
                "##### H5\n" +
                "###### H6\n" +
                "####### H7\n" +
                "######## H8\n" +
                "==multi line\n" +
                "highl=ight=\\=there is more shit==\n" +
                "> quote1\n" +
                "> quote2\n" +
                "\n" +
                "[example link](http://example.com/)\n" +
                "![big](https://via.placeholder.com/350x150)\n" +
                "-----\n" +
                "![small](https://via.placeholder.com/250x150)\n" +
                "[![image link](https://via.placeholder.com/150x150)](http://example.com/)\n" +
                "{{}} <- empty icon\n" +
                "{{bath}} <- here is an icon\n" +
                "{{imdb}} <- another icon\n" +
                "{iframe}() <- empty iframe\n" +
                "{iframe}(https://www.youtu.be/y6120QOlsfU)\n" +
                "{iframe}(https://www.youtube.com/watch?v=y6120QOlsfU)\n" +
                "{iframe}(https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d21614.96154166493!2d8.546484630964708!3d47.37545193358426!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x479aa0a6516e8cf7%3A0x4b61f428b6377607!2sETH+Z%C3%BCrich!5e0!3m2!1sen!2sch!4v1519212311773)";


        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.withDescription(testString);

        GECkO.mainChannel.sendMessage(MediaSynchronizer.processMarkdown(embedBuilder.build()));*/

        MediaSynchronizer.loadNews();
    }
}
