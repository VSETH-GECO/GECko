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
import discord4j.core.object.entity.Message;

import java.util.List;

public class Restart extends Command {
    public Restart() {
        this.setName("restart");
        this.setDescription("Restarts the bot.");
    }

    @Override
    public void execute(Message msg, List<String> args) {
        CommandUtils.respond(msg, "**Restarting bot...**").subscribe();

        GECkO.logger.debug("[Restart] - Logging out...");
        GECkO.discordClient.logout();
        GECkO.logger.debug("[Restart] - calling System.exit(0)...");
        new Thread(() -> System.exit(0)).start();
    }
}
