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

import ch.ethz.geco.g4j.obj.ILanUser;
import ch.ethz.geco.gecko.GECkO;
import ch.ethz.geco.gecko.command.Command;
import ch.ethz.geco.gecko.command.CommandUtils;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

import java.util.List;
import java.util.NoSuchElementException;

public class Whois extends Command {
    public Whois() {
        this.setName("whois");
        this.setParams("<@User>");
        this.setDescription("Returns information about the given user.");
    }

    @Override
    public void execute(IMessage msg, List<String> args) {
        if (msg.getMentions().size() > 0) {
            IUser user = msg.getMentions().get(0);
            String message;
            try {
                ch.ethz.geco.g4j.obj.IUser userInfo = GECkO.gecoClient.getUserByDiscordID(user.getLongID());
                if (userInfo != null) {
                    message = "**__User: " + user.getName() + "#" + user.getDiscriminator() + "__**\n**GECO:** <https://geco.ethz.ch/user/" + userInfo.getID() + ">";

                    if (userInfo.getSteamID().isPresent()) {
                        message += "\n**Steam:** <http://steamcommunity.com/profiles/" + userInfo.getSteamID().get() + ">";
                    }

                    if (userInfo.getBattleNetID().isPresent()) {
                        message += "\n**Battle.net:** " + userInfo.getBattleNetID().get();
                    }

                    ILanUser lanUser = GECkO.gecoClient.getLanUserByName(userInfo.getUserName());
                    if (lanUser != null) {
                        if (lanUser.getSeatName().isPresent() && !lanUser.getSeatName().get().equals("")) {
                            message += "\n**Seat:** " + lanUser.getSeatName().get();
                        }
                    }
                } else {
                    message = "An internal error occurred.";
                }
            } catch (NoSuchElementException e) {
                message = "There is no account linked to **" + user.getName() + "#" + user.getDiscriminator() + "**";
            }

            CommandUtils.respond(msg, message);
        } else {
            printUsage(msg);
        }
    }
}
