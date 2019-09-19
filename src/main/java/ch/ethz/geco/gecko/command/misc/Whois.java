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

import ch.ethz.geco.g4j.obj.LanUser;
import ch.ethz.geco.g4j.obj.User;
import ch.ethz.geco.gecko.GECko;
import ch.ethz.geco.gecko.command.Command;
import ch.ethz.geco.gecko.command.CommandUtils;
import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Snowflake;

import java.util.List;
import java.util.NoSuchElementException;

public class Whois extends Command {
    public Whois() {
        this.setName("whois");
        this.setParams("<@User>");
        this.setDescription("Returns information about the given user.");
    }

    @Override
    public void execute(Message msg, List<String> args) {
        if (!msg.getUserMentionIds().isEmpty()) {
            Snowflake userID = (Snowflake) msg.getUserMentionIds().toArray()[0];
            String message;
            try {
                User userInfo = GECko.gecoClient.getUserByDiscordID(userID.asLong()).block();
                if (userInfo != null) {
                    message = "\n**GECO:** <https://geco.ethz.ch/user/" + userInfo.getID() + ">";

                    if (userInfo.getSteamID().isPresent()) {
                        message += "\n**Steam:** <http://steamcommunity.com/profiles/" + userInfo.getSteamID().get() + ">";
                    }

                    if (userInfo.getBattleNetID().isPresent()) {
                        message += "\n**Battle.net:** " + userInfo.getBattleNetID().get();
                    }

                    LanUser lanUser = GECko.gecoClient.getLanUserByName(userInfo.getUserName()).block();
                    if (lanUser != null) {
                        if (lanUser.getSeatName().isPresent() && !lanUser.getSeatName().get().equals("")) {
                            message += "\n**Seat:** " + lanUser.getSeatName().get();
                        }
                    }
                } else {
                    message = "An internal error occurred.";
                }
            } catch (NoSuchElementException e) {
                message = "";
            }

            final String finalMessage = message;
            GECko.discordClient.getUserById(userID).subscribe(user -> {
                String newMessage = finalMessage;
                if (newMessage.length() > 0) {
                    newMessage = "**__User: " + user.getUsername() + "#" + user.getDiscriminator() + "__**" + newMessage;
                } else {
                    newMessage = "There is no account linked to **" + user.getUsername() + "#" + user.getDiscriminator() + "**";
                }

                CommandUtils.respond(msg, newMessage).subscribe();
            });
            CommandUtils.respond(msg, message).subscribe();
        } else {
            printUsage(msg).subscribe();
        }
    }
}
