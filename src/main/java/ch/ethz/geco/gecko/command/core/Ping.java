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

import ch.ethz.geco.gecko.command.Command;
import ch.ethz.geco.gecko.command.CommandUtils;
import sx.blah.discord.handle.obj.IMessage;

import java.time.ZoneOffset;
import java.util.List;

/**
 * Really simple command to check if bot is still responsive
 */
public class Ping extends Command {
    public Ping() {
        this.setNames(new String[]{"ping", "p"});
        this.setDescription("Used to test if bot is still responsive.");
    }

    @Override
    public void execute(IMessage msg, List<String> args) {
        IMessage pongMsg = CommandUtils.respond(msg, "Pong!");

        long millis = pongMsg.getTimestamp().atZone(ZoneOffset.UTC).toInstant().toEpochMilli() - msg.getTimestamp().atZone(ZoneOffset.UTC).toInstant().toEpochMilli();

        CommandUtils.editMessage(pongMsg, "Pong! <" + millis + "ms>");
    }
}
