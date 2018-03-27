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

import ch.ethz.geco.gecko.command.Command;
import ch.ethz.geco.gecko.command.CommandUtils;
import ch.ethz.geco.gecko.rest.api.geco.GecoAPI;
import ch.ethz.geco.gecko.rest.api.geco.UserInfo;
import org.apache.commons.lang3.StringUtils;
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
                UserInfo userInfo = GecoAPI.getUserInfoByDiscordID(user.getLongID());
                if (userInfo != null) {
                    message = "**__User: " + user.getName() + "#" + user.getDiscriminator() + "__**\n**GECO:** <https://geco.ethz.ch/user/" + userInfo.getWebID() + ">";

                    String steamAccount = userInfo.getAccount(UserInfo.AccountType.STEAM);
                    if (steamAccount != null) {
                        message += "\n**Steam:** <http://steamcommunity.com/profiles/" + steamAccount + ">";
                    }

                    String blizzardAccount = userInfo.getAccount(UserInfo.AccountType.BLIZZARD);
                    if (blizzardAccount != null) {
                        message += "\n**Battle.net:** " + blizzardAccount;
                    }

                    if (GecoAPI.loadLanUser(userInfo)) {
                        if (userInfo.getSeat() != null && !userInfo.getSeat().equals("")) {
                            message += "\n**Seat:** " + userInfo.getSeat();
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
